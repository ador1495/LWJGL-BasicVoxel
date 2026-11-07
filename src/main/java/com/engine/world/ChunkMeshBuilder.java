package com.engine.world;

import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;

public class ChunkMeshBuilder {
    // Reusable buffers per thread to avoid GC churn
    private static final int INITIAL_CAPACITY = 16384;
    private int[] vertices = new int[INITIAL_CAPACITY];
    private int[] indices = new int[INITIAL_CAPACITY * 6 / 4];
    private int vertexCount = 0;
    private int indexCount = 0;

    public static class MeshData {
        public IntBuffer vertexBuffer;
        public IntBuffer indexBuffer;
        public int indexCount;
    }

    public MeshData buildMesh(Chunk chunk, int sectionIdx) {
        ChunkSection section = chunk.getSection(sectionIdx);
        if (section == null || section.isEmpty()) {
            return null;
        }

        vertexCount = 0;
        indexCount = 0;

        int sectionBaseY = sectionIdx * ChunkSection.SIZE;

        for (int y = 0; y < ChunkSection.SIZE; y++) {
            int worldY = sectionBaseY + y;
            for (int z = 0; z < ChunkSection.SIZE; z++) {
                for (int x = 0; x < ChunkSection.SIZE; x++) {
                    byte blockId = section.getBlock(x, y, z);
                    if (blockId == 0) continue;

                    Block block = Block.getById(blockId);

                    // Test all 6 faces
                    // 1. UP (+Y)
                    testFace(chunk, block, x, worldY, z, y, Block.FACE_UP, 0, 1, 0);
                    // 2. DOWN (-Y)
                    testFace(chunk, block, x, worldY, z, y, Block.FACE_DOWN, 0, -1, 0);
                    // 3. NORTH (-Z)
                    testFace(chunk, block, x, worldY, z, y, Block.FACE_NORTH, 0, 0, -1);
                    // 4. SOUTH (+Z)
                    testFace(chunk, block, x, worldY, z, y, Block.FACE_SOUTH, 0, 0, 1);
                    // 5. WEST (-X)
                    testFace(chunk, block, x, worldY, z, y, Block.FACE_WEST, -1, 0, 0);
                    // 6. EAST (+X)
                    testFace(chunk, block, x, worldY, z, y, Block.FACE_EAST, 1, 0, 0);
                }
            }
        }

        if (indexCount == 0) {
            return null;
        }

        MeshData data = new MeshData();
        data.indexCount = indexCount;
        data.vertexBuffer = BufferUtils.createIntBuffer(vertexCount);
        data.vertexBuffer.put(vertices, 0, vertexCount).flip();

        data.indexBuffer = BufferUtils.createIntBuffer(indexCount);
        data.indexBuffer.put(indices, 0, indexCount).flip();

        return data;
    }

    private void testFace(Chunk chunk, Block block, int x, int worldY, int z, int localY, int face, int dx, int dy, int dz) {
        byte neighborId = chunk.getBlock(x + dx, worldY + dy, z + dz);
        Block neighbor = Block.getById(neighborId);

        boolean shouldRender = false;
        if (neighbor == Block.AIR) {
            shouldRender = true;
        } else if (block.transparent) {
            // Transparent blocks like glass or water don't render against the same block
            if (neighbor != block && neighbor.transparent) {
                shouldRender = true;
            }
        } else if (neighbor.transparent) {
            // Solid block next to transparent block (leaves, water, glass)
            shouldRender = true;
        }

        if (shouldRender) {
            int tex = block.getTexture(face);
            addQuad(chunk, x, localY, z, worldY, face, tex);
        }
    }

    private void addQuad(Chunk chunk, int x, int y, int z, int worldY, int face, int tex) {
        ensureCapacity(4, 6);

        int baseIndex = vertexCount;

        // Coordinates:
        int x0 = x, x1 = x + 1;
        int y0 = y, y1 = y + 1;
        int z0 = z, z1 = z + 1;

        int ao0, ao1, ao2, ao3;

        switch (face) {
            case Block.FACE_UP -> { // +Y
                ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x - 1, worldY + 1, z + 1));
                ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x + 1, worldY + 1, z + 1));
                ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x + 1, worldY + 1, z - 1));
                ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x - 1, worldY + 1, z - 1));

                vertices[vertexCount++] = pack(x0, y1, z1, face, tex, ao0, 0);
                vertices[vertexCount++] = pack(x1, y1, z1, face, tex, ao1, 1);
                vertices[vertexCount++] = pack(x1, y1, z0, face, tex, ao2, 2);
                vertices[vertexCount++] = pack(x0, y1, z0, face, tex, ao3, 3);
            }
            case Block.FACE_DOWN -> { // -Y
                ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x - 1, worldY - 1, z - 1));
                ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x + 1, worldY - 1, z - 1));
                ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x + 1, worldY - 1, z + 1));
                ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x - 1, worldY - 1, z + 1));

                vertices[vertexCount++] = pack(x0, y0, z0, face, tex, ao0, 0);
                vertices[vertexCount++] = pack(x1, y0, z0, face, tex, ao1, 1);
                vertices[vertexCount++] = pack(x1, y0, z1, face, tex, ao2, 2);
                vertices[vertexCount++] = pack(x0, y0, z1, face, tex, ao3, 3);
            }
            case Block.FACE_NORTH -> { // -Z
                ao0 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x + 1, worldY - 1, z - 1));
                ao1 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x - 1, worldY - 1, z - 1));
                ao2 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x - 1, worldY + 1, z - 1));
                ao3 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x + 1, worldY + 1, z - 1));

                vertices[vertexCount++] = pack(x1, y0, z0, face, tex, ao0, 0);
                vertices[vertexCount++] = pack(x0, y0, z0, face, tex, ao1, 1);
                vertices[vertexCount++] = pack(x0, y1, z0, face, tex, ao2, 2);
                vertices[vertexCount++] = pack(x1, y1, z0, face, tex, ao3, 3);
            }
            case Block.FACE_SOUTH -> { // +Z
                ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x - 1, worldY - 1, z + 1));
                ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x + 1, worldY - 1, z + 1));
                ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x + 1, worldY + 1, z + 1));
                ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x - 1, worldY + 1, z + 1));

                vertices[vertexCount++] = pack(x0, y0, z1, face, tex, ao0, 0);
                vertices[vertexCount++] = pack(x1, y0, z1, face, tex, ao1, 1);
                vertices[vertexCount++] = pack(x1, y1, z1, face, tex, ao2, 2);
                vertices[vertexCount++] = pack(x0, y1, z1, face, tex, ao3, 3);
            }
            case Block.FACE_WEST -> { // -X (Normal = -1, 0, 0)
                ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x - 1, worldY - 1, z - 1));
                ao1 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x - 1, worldY - 1, z + 1));
                ao2 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x - 1, worldY + 1, z + 1));
                ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x - 1, worldY + 1, z - 1));

                vertices[vertexCount++] = pack(x0, y0, z0, face, tex, ao0, 0);
                vertices[vertexCount++] = pack(x0, y0, z1, face, tex, ao1, 1);
                vertices[vertexCount++] = pack(x0, y1, z1, face, tex, ao2, 2);
                vertices[vertexCount++] = pack(x0, y1, z0, face, tex, ao3, 3);
            }
            case Block.FACE_EAST -> { // +X (Normal = +1, 0, 0)
                ao0 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x + 1, worldY - 1, z + 1));
                ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x + 1, worldY - 1, z - 1));
                ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x + 1, worldY + 1, z - 1));
                ao3 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x + 1, worldY + 1, z + 1));

                vertices[vertexCount++] = pack(x1, y0, z1, face, tex, ao0, 0);
                vertices[vertexCount++] = pack(x1, y0, z0, face, tex, ao1, 1);
                vertices[vertexCount++] = pack(x1, y1, z0, face, tex, ao2, 2);
                vertices[vertexCount++] = pack(x1, y1, z1, face, tex, ao3, 3);
            }
            default -> {
                ao0 = 3; ao1 = 3; ao2 = 3; ao3 = 3;
            }
        }

        // Triangulate quad: flip diagonal if ao0 + ao2 > ao1 + ao3 to fix anisotropy
        if (ao0 + ao2 > ao1 + ao3) {
            indices[indexCount++] = baseIndex;
            indices[indexCount++] = baseIndex + 1;
            indices[indexCount++] = baseIndex + 2;

            indices[indexCount++] = baseIndex;
            indices[indexCount++] = baseIndex + 2;
            indices[indexCount++] = baseIndex + 3;
        } else {
            indices[indexCount++] = baseIndex + 1;
            indices[indexCount++] = baseIndex + 2;
            indices[indexCount++] = baseIndex + 3;

            indices[indexCount++] = baseIndex + 1;
            indices[indexCount++] = baseIndex + 3;
            indices[indexCount++] = baseIndex;
        }
    }

    private static boolean isSolid(Chunk chunk, int x, int y, int z) {
        byte b = chunk.getBlock(x, y, z);
        return b != 0 && Block.getById(b).solid && !Block.getById(b).transparent;
    }

    private static int getAO(Chunk chunk, boolean side1, boolean side2, boolean corner) {
        if (side1 && side2) {
            return 0;
        }
        int count = 0;
        if (side1) count++;
        if (side2) count++;
        if (corner) count++;
        return 3 - count;
    }

    private static int pack(int x, int y, int z, int face, int tex, int ao, int uv) {
        return (x & 0x1F) |
               ((y & 0x1F) << 5) |
               ((z & 0x1F) << 10) |
               ((face & 0x07) << 15) |
               ((tex & 0x7F) << 18) |
               ((ao & 0x03) << 25) |
               ((uv & 0x03) << 27);
    }

    private void ensureCapacity(int additionalVerts, int additionalIndices) {
        if (vertexCount + additionalVerts > vertices.length) {
            int[] newVerts = new int[vertices.length * 2];
            System.arraycopy(vertices, 0, newVerts, 0, vertexCount);
            vertices = newVerts;
        }
        if (indexCount + additionalIndices > indices.length) {
            int[] newIndices = new int[indices.length * 2];
            System.arraycopy(indices, 0, newIndices, 0, indexCount);
            indices = newIndices;
        }
    }
}

