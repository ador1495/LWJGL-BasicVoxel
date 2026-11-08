package com.engine.world;

import com.engine.core.Camera;
import com.engine.core.Frustum;
import com.engine.gfx.Mesh;
import com.engine.gfx.Shader;
import com.engine.gfx.TextureArray;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

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
    private final List<ChunkSection> visibleSections = new ArrayList<>(1024);

    public static class BlockPos {
        public final int x, y, z;
        public BlockPos(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPos p)) return false;
            return x == p.x && y == p.y && z == p.z;
        }
        @Override public int hashCode() {
            return 31 * (31 * x + y) + z;
        }
    }
    private final Set<BlockPos> placedTorches = ConcurrentHashMap.newKeySet();

    private int renderedSectionsCount = 0;
    private int culledSectionsCount = 0;
    private int totalSectionsCount = 0;

    private static class MeshTaskResult {
        final ChunkSection section;
        final ChunkMeshBuilder.SectionMeshData data;

        MeshTaskResult(ChunkSection section, ChunkMeshBuilder.SectionMeshData data) {
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
            chunk.setWorld(this);
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
                if (result.data.opaque != null) {
                    result.section.getOpaqueMesh().upload(result.data.opaque.vertexBuffer, result.data.opaque.indexBuffer, result.data.opaque.indexCount);
                } else {
                    result.section.getOpaqueMesh().cleanup();
                }
                if (result.data.translucent != null) {
                    result.section.getTranslucentMesh().upload(result.data.translucent.vertexBuffer, result.data.translucent.indexBuffer, result.data.translucent.indexCount);
                } else {
                    result.section.getTranslucentMesh().cleanup();
                }
            } else {
                result.section.cleanup();
            }
            result.section.setBuilding(false);
            uploads++;
        }

        // 2. Ensure chunks in render distance + 1 are generated so neighbors exist for meshing
        int loadDist = renderDistance + 1;
        for (int dx = -loadDist; dx <= loadDist; dx++) {
            for (int dz = -loadDist; dz <= loadDist; dz++) {
                if (dx * dx + dz * dz > (loadDist + 0.5f) * (loadDist + 0.5f)) {
                    continue;
                }
                getOrCreateChunk(playerChunkX + dx, playerChunkZ + dz);
            }
        }

        // 3. Queue dirty sections for meshing ONLY when fully wired with all 4 neighbors
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                if (dx * dx + dz * dz > (renderDistance + 0.5f) * (renderDistance + 0.5f)) {
                    continue;
                }

                Chunk chunk = getChunk(playerChunkX + dx, playerChunkZ + dz);
                if (chunk == null || !chunk.isGenerated() || !chunk.isFullyWired()) {
                    continue;
                }

                for (int y = 0; y < Chunk.SECTIONS_COUNT; y++) {
                    ChunkSection section = chunk.getSection(y);
                    if (section.isDirty() && !section.isBuilding()) {
                        if (section.isEmpty()) {
                            section.cleanup();
                            section.setDirty(false);
                            continue;
                        }

                        section.setBuilding(true);
                        final int sectionIdx = y;
                        executor.submit(() -> {
                            section.setDirty(false);
                            ChunkMeshBuilder builder = new ChunkMeshBuilder();
                            ChunkMeshBuilder.SectionMeshData data = builder.buildMesh(chunk, sectionIdx);
                            uploadQueue.offer(new MeshTaskResult(section, data));
                        });
                    }
                }
            }
        }
    }

    public void render(Camera camera, Shader shader, TextureArray textures, DaylightCycle daylightCycle, Vector3f playerPos, boolean holdingTorch) {
        Frustum frustum = camera.getFrustum();
        shader.bind();
        shader.setUniform("u_ViewProjection", camera.getViewProjectionMatrix());
        shader.setUniform("u_CameraPos", camera.getPosition());
        shader.setUniform("u_PlayerPos", playerPos);
        shader.setUniform("u_TextureArray", 0);
        textures.bindToUnit(0);

        // Dynamic handheld torchlight
        if (holdingTorch) {
            Vector3f heldPos = new Vector3f(playerPos).add(0.0f, 1.2f, 0.0f);
            float flicker = 1.0f + 0.04f * (float) Math.sin(com.engine.core.Time.getTime() * 14.0);
            shader.setUniform("u_HeldTorchPos", heldPos);
            shader.setUniform("u_HeldTorchLight", new Vector3f(1.0f * flicker, 0.68f * flicker, 0.28f * flicker));
        } else {
            shader.setUniform("u_HeldTorchLight", new Vector3f(0.0f, 0.0f, 0.0f));
        }

        // Dynamic placed torchlights (up to 16 closest within radius)
        int torchCount = 0;
        for (BlockPos p : placedTorches) {
            if (torchCount >= 16) break;
            float dx = p.x + 0.5f - camera.getPosition().x;
            float dy = p.y + 0.5f - camera.getPosition().y;
            float dz = p.z + 0.5f - camera.getPosition().z;
            if (dx * dx + dy * dy + dz * dz < 32.0f * 32.0f) {
                shader.setUniform("u_PlacedTorches[" + torchCount + "]", p.x + 0.5f, p.y + 0.5f, p.z + 0.5f);
                torchCount++;
            }
        }
        shader.setUniform("u_NumPlacedTorches", torchCount);
        shader.setUniform("u_Time", (float) com.engine.core.Time.getTime());

        // Dynamic lighting uniforms from DaylightCycle
        shader.setUniform("u_LightDir", daylightCycle.getLightDir());
        shader.setUniform("u_LightColor", daylightCycle.getLightColor());
        shader.setUniform("u_AmbientColor", daylightCycle.getAmbientColor());
        shader.setUniform("u_FogColor", daylightCycle.getSkyColor());

        // Distance fog parameters
        float fogEnd = (renderDistance - 0.5f) * Chunk.CHUNK_SIZE;
        float fogStart = fogEnd * 0.70f;
        shader.setUniform("u_FogStart", fogStart);
        shader.setUniform("u_FogEnd", fogEnd);

        renderedSectionsCount = 0;
        culledSectionsCount = 0;
        totalSectionsCount = 0;
        visibleSections.clear();

        // 1. Frustum culling pass
        for (Chunk chunk : chunks.values()) {
            for (int y = 0; y < Chunk.SECTIONS_COUNT; y++) {
                ChunkSection section = chunk.getSection(y);
                if (!section.hasMesh()) continue;

                totalSectionsCount++;

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

                visibleSections.add(section);
                renderedSectionsCount++;
            }
        }

        // 2. Pass 1: Opaque geometry (Solid blocks, dirt, grass, stone, wood, etc.)
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

        for (int i = 0; i < visibleSections.size(); i++) {
            ChunkSection section = visibleSections.get(i);
            Mesh opaque = section.getOpaqueMesh();
            if (!opaque.isEmpty()) {
                shader.setUniform("u_ChunkOffset", (float) section.getWorldX(), (float) section.getWorldY(), (float) section.getWorldZ());
                opaque.render();
            }
        }

        // 3. Pass 2: Translucent geometry (Water with Fresnel & transparency, depth write disabled)
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false); // Prevents water from occluding the riverbed/seabed

        for (int i = 0; i < visibleSections.size(); i++) {
            ChunkSection section = visibleSections.get(i);
            Mesh trans = section.getTranslucentMesh();
            if (!trans.isEmpty()) {
                shader.setUniform("u_ChunkOffset", (float) section.getWorldX(), (float) section.getWorldY(), (float) section.getWorldZ());
                trans.render();
            }
        }

        // Restore OpenGL state
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
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

        // Update placed torch tracking
        BlockPos pos = new BlockPos(worldX, worldY, worldZ);
        if (blockId == Block.TORCH.id) {
            placedTorches.add(pos);
        } else {
            placedTorches.remove(pos);
        }

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

    public int getTorchLightAt(int worldX, int worldY, int worldZ) {
        if (placedTorches.isEmpty()) return 0;
        int maxL = 0;
        for (BlockPos p : placedTorches) {
            int dist = Math.abs(worldX - p.x) + Math.abs(worldY - p.y) + Math.abs(worldZ - p.z);
            if (dist < 14) {
                int l = 14 - dist;
                if (l > maxL) {
                    maxL = l;
                    if (maxL >= 14) break;
                }
            }
        }
        return maxL;
    }

    private void rebuildSectionImmediate(Chunk chunk, int sectionIdx) {
        ChunkSection section = chunk.getSection(sectionIdx);
        if (section == null) return;
        if (section.isEmpty()) {
            section.cleanup();
            section.setDirty(false);
            return;
        }
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        ChunkMeshBuilder.SectionMeshData data = builder.buildMesh(chunk, sectionIdx);
        if (data != null) {
            if (data.opaque != null) {
                section.getOpaqueMesh().upload(data.opaque.vertexBuffer, data.opaque.indexBuffer, data.opaque.indexCount);
            } else {
                section.getOpaqueMesh().cleanup();
            }
            if (data.translucent != null) {
                section.getTranslucentMesh().upload(data.translucent.vertexBuffer, data.translucent.indexBuffer, data.translucent.indexCount);
            } else {
                section.getTranslucentMesh().cleanup();
            }
        } else {
            section.cleanup();
        }
        section.setDirty(false);
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
