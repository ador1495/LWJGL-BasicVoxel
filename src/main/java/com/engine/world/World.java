package com.engine.world;

import com.engine.core.Camera;
import com.engine.core.Frustum;
import com.engine.gfx.Mesh;
import com.engine.gfx.Shader;
import com.engine.gfx.TextureArray;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.*;

public class World {
    public static final int DEFAULT_RENDER_DISTANCE = 8;
    private int renderDistance = DEFAULT_RENDER_DISTANCE;
    private static final int MAX_UPLOADS_PER_FRAME = 32;

    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final WorldGenerator generator;
    private final ExecutorService executor;
    private final ConcurrentLinkedQueue<MeshTaskResult> uploadQueue = new ConcurrentLinkedQueue<>();
    private final Set<ChunkSection> queuedSections = ConcurrentHashMap.newKeySet();

    private int renderedSectionsCount = 0;
    private int culledSectionsCount = 0;
    private int totalSectionsCount = 0;

    private static class MeshTaskResult {
        final ChunkSection section;
        final ChunkMeshBuilder.MeshData data;

        MeshTaskResult(ChunkSection section, ChunkMeshBuilder.MeshData data) {
            this.section = section;
            this.data = data;
        }
    }

    public World(long seed) {
        this.generator = new WorldGenerator(seed);
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "Voxel-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public static long chunkKey(int x, int z) {
        return (((long) x) << 32) | (z & 0xFFFFFFFFL);
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        return chunks.computeIfAbsent(key, k -> {
            Chunk chunk = new Chunk(chunkX, chunkZ);
            wireNeighbors(chunk, chunkX, chunkZ);
            generator.generateChunk(chunk);
            return chunk;
        });
    }

    private void wireNeighbors(Chunk chunk, int x, int z) {
        Chunk n = chunks.get(chunkKey(x, z - 1));
        Chunk s = chunks.get(chunkKey(x, z + 1));
        Chunk w = chunks.get(chunkKey(x - 1, z));
        Chunk e = chunks.get(chunkKey(x + 1, z));

        chunk.north = n; if (n != null) { n.south = chunk; markAllSectionsDirty(n); }
        chunk.south = s; if (s != null) { s.north = chunk; markAllSectionsDirty(s); }
        chunk.west = w;  if (w != null) { w.east = chunk; markAllSectionsDirty(w); }
        chunk.east = e;  if (e != null) { e.west = chunk; markAllSectionsDirty(e); }
    }

    private void markAllSectionsDirty(Chunk chunk) {
        for (int y = 0; y < Chunk.SECTIONS_COUNT; y++) {
            ChunkSection s = chunk.getSection(y);
            if (s != null && !s.isEmpty()) {
                s.setDirty(true);
            }
        }
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(int rd) {
        this.renderDistance = Math.max(2, Math.min(32, rd));
    }

    public void changeRenderDistance(int delta) {
        setRenderDistance(this.renderDistance + delta);
    }

    public void update(Vector3f playerPos) {
        int playerChunkX = (int) Math.floor(playerPos.x / Chunk.CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerPos.z / Chunk.CHUNK_SIZE);

        // 1. Process mesh uploads on main thread
        int uploads = 0;
        MeshTaskResult result;
        while (uploads < MAX_UPLOADS_PER_FRAME && (result = uploadQueue.poll()) != null) {
            if (result.data != null) {
                result.section.getMesh().upload(result.data.vertexBuffer, result.data.indexBuffer, result.data.indexCount);
            } else {
                result.section.getMesh().cleanup();
            }
            result.section.setDirty(false);
            queuedSections.remove(result.section);
            uploads++;
        }

        // 2. Load chunks in render distance
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;

                // Circular distance check
                if (dx * dx + dz * dz > (renderDistance + 0.5f) * (renderDistance + 0.5f)) {
                    continue;
                }

                Chunk chunk = getOrCreateChunk(cx, cz);

                // Queue dirty sections for meshing
                for (int y = 0; y < Chunk.SECTIONS_COUNT; y++) {
                    ChunkSection section = chunk.getSection(y);
                    if (section.isDirty() && !queuedSections.contains(section)) {
                        if (section.isEmpty()) {
                            section.getMesh().cleanup();
                            section.setDirty(false);
                            continue;
                        }

                        queuedSections.add(section);
                        final int sectionIdx = y;
                        executor.submit(() -> {
                            ChunkMeshBuilder builder = new ChunkMeshBuilder();
                            ChunkMeshBuilder.MeshData data = builder.buildMesh(chunk, sectionIdx);
                            uploadQueue.offer(new MeshTaskResult(section, data));
                        });
                    }
                }
            }
        }
    }

    public void render(Camera camera, Shader shader, TextureArray textures) {
        Frustum frustum = camera.getFrustum();
        shader.bind();
        shader.setUniform("u_ViewProjection", camera.getViewProjectionMatrix());
        shader.setUniform("u_CameraPos", camera.getPosition());
        shader.setUniform("u_TextureArray", 0);
        textures.bindToUnit(0);

        // Distance fog parameters
        float fogEnd = (renderDistance - 0.5f) * Chunk.CHUNK_SIZE;
        float fogStart = fogEnd * 0.70f;
        shader.setUniform("u_FogStart", fogStart);
        shader.setUniform("u_FogEnd", fogEnd);
        shader.setUniform("u_FogColor", 0.65f, 0.82f, 1.0f); // Bright sky blue

        renderedSectionsCount = 0;
        culledSectionsCount = 0;
        totalSectionsCount = 0;

        for (Chunk chunk : chunks.values()) {
            for (int y = 0; y < Chunk.SECTIONS_COUNT; y++) {
                ChunkSection section = chunk.getSection(y);
                Mesh mesh = section.getMesh();
                if (mesh.isEmpty()) continue;

                totalSectionsCount++;

                // Frustum Culling
                float minX = section.getWorldX();
                float minY = section.getWorldY();
                float minZ = section.getWorldZ();
                float maxX = minX + ChunkSection.SIZE;
                float maxY = minY + ChunkSection.SIZE;
                float maxZ = minZ + ChunkSection.SIZE;

                if (!frustum.testAABB(minX, minY, minZ, maxX, maxY, maxZ)) {
                    culledSectionsCount++;
                    continue;
                }

                shader.setUniform("u_ChunkOffset", minX, minY, minZ);
                mesh.render();
                renderedSectionsCount++;
            }
        }

        shader.unbind();
    }

    public byte getBlock(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.CHUNK_HEIGHT) return 0;
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return 0;

        int lx = worldX & 15;
        int lz = worldZ & 15;
        return chunk.getBlock(lx, worldY, lz);
    }

    public void setBlock(int worldX, int worldY, int worldZ, byte blockId) {
        if (worldY < 0 || worldY >= Chunk.CHUNK_HEIGHT) return;
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        Chunk chunk = getOrCreateChunk(cx, cz);

        int lx = worldX & 15;
        int lz = worldZ & 15;
        chunk.setBlock(lx, worldY, lz, blockId);

        // Immediate responsive rebuild for player interaction (sub-0.1ms)
        int sectionIdx = worldY >> 4;
        rebuildSectionImmediate(chunk, sectionIdx);

        if ((worldY & 15) == 0 && sectionIdx > 0) rebuildSectionImmediate(chunk, sectionIdx - 1);
        if ((worldY & 15) == 15 && sectionIdx < Chunk.SECTIONS_COUNT - 1) rebuildSectionImmediate(chunk, sectionIdx + 1);

        if (lx == 0 && chunk.west != null) rebuildSectionImmediate(chunk.west, sectionIdx);
        if (lx == 15 && chunk.east != null) rebuildSectionImmediate(chunk.east, sectionIdx);
        if (lz == 0 && chunk.north != null) rebuildSectionImmediate(chunk.north, sectionIdx);
        if (lz == 15 && chunk.south != null) rebuildSectionImmediate(chunk.south, sectionIdx);
    }

    private void rebuildSectionImmediate(Chunk chunk, int sectionIdx) {
        ChunkSection section = chunk.getSection(sectionIdx);
        if (section == null) return;
        if (section.isEmpty()) {
            section.getMesh().cleanup();
            section.setDirty(false);
            return;
        }
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        ChunkMeshBuilder.MeshData data = builder.buildMesh(chunk, sectionIdx);
        if (data != null) {
            section.getMesh().upload(data.vertexBuffer, data.indexBuffer, data.indexCount);
        } else {
            section.getMesh().cleanup();
        }
        section.setDirty(false);
        queuedSections.remove(section);
    }

    public int getRenderedSectionsCount() {
        return renderedSectionsCount;
    }

    public int getCulledSectionsCount() {
        return culledSectionsCount;
    }

    public int getTotalSectionsCount() {
        return totalSectionsCount;
    }

    public int getLoadedChunksCount() {
        return chunks.size();
    }

    public void cleanup() {
        executor.shutdownNow();
        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();
    }
}
