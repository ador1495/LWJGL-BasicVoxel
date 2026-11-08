package com.engine.world;

import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;

public class ChunkMeshBuilder {
    private static final int INITIAL_CAPACITY = 16384;

    // Opaque buffers
    private int[] opaqueVertices = new int[INITIAL_CAPACITY];
    private int[] opaqueIndices = new int[INITIAL_CAPACITY * 6 / 4];
    private int opaqueVertexCount = 0;
    private int opaqueIndexCount = 0;

    // Translucent (Water) buffers
    private int[] transVertices = new int[INITIAL_CAPACITY / 2];
    private int[] transIndices = new int[(INITIAL_CAPACITY / 2) * 6 / 4];
    private int transVertexCount = 0;
    private int transIndexCount = 0;

    public static class MeshData {
        public IntBuffer vertexBuffer;
        public IntBuffer indexBuffer;
        public int indexCount;
    }

    public static class SectionMeshData {
        public MeshData opaque;
        public MeshData translucent;
    }

    public SectionMeshData buildMesh(Chunk chunk, int sectionIdx) {
        ChunkSection section = chunk.getSection(sectionIdx);
        if (section == null || section.isEmpty()) {
            return null;
        }

        opaqueVertexCount = 0;
        opaqueIndexCount = 0;
        transVertexCount = 0;
        transIndexCount = 0;

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

        if (opaqueIndexCount == 0 && transIndexCount == 0) {
            return null;
        }

        SectionMeshData data = new SectionMeshData();

        if (opaqueIndexCount > 0) {
            data.opaque = new MeshData();
            data.opaque.indexCount = opaqueIndexCount;
            data.opaque.vertexBuffer = BufferUtils.createIntBuffer(opaqueVertexCount);
            data.opaque.vertexBuffer.put(opaqueVertices, 0, opaqueVertexCount).flip();

            data.opaque.indexBuffer = BufferUtils.createIntBuffer(opaqueIndexCount);
            data.opaque.indexBuffer.put(opaqueIndices, 0, opaqueIndexCount).flip();
        }

        if (transIndexCount > 0) {
            data.translucent = new MeshData();
            data.translucent.indexCount = transIndexCount;
            data.translucent.vertexBuffer = BufferUtils.createIntBuffer(transVertexCount);
            data.translucent.vertexBuffer.put(transVertices, 0, transVertexCount).flip();

            data.translucent.indexBuffer = BufferUtils.createIntBuffer(transIndexCount);
            data.translucent.indexBuffer.put(transIndices, 0, transIndexCount).flip();
        }

        return data;
    }

    private void testFace(Chunk chunk, Block block, int x, int worldY, int z, int localY, int face, int dx, int dy, int dz) {
        byte neighborId = chunk.getBlock(x + dx, worldY + dy, z + dz);
        Block neighbor = Block.getById(neighborId);

        boolean shouldRender = false;
        if (block == Block.TORCH) {
            shouldRender = (neighbor != Block.TORCH);
        } else if (block == Block.WATER) {
            if (neighbor == Block.AIR) {
                if (face == Block.FACE_UP) {
                    shouldRender = true;
                } else if (face == Block.FACE_DOWN) {
                    shouldRender = (worldY > WorldGenerator.SEA_LEVEL);
                } else {
                    // Side face of water: only render if next to true air at or above sea level
                    shouldRender = (worldY >= WorldGenerator.SEA_LEVEL);
                }
            }
        } else if (neighbor == Block.AIR) {
            shouldRender = true;
        } else if (block.transparent) {
            if (neighbor != block && neighbor.transparent) {
                shouldRender = true;
            }
        } else if (neighbor.transparent) {
            shouldRender = true;
        }

        if (shouldRender) {
            int tex = block.getTexture(face);
            boolean isTranslucent = (block == Block.WATER);
            boolean isTorch = (block == Block.TORCH);
            addQuad(chunk, x, localY, z, worldY, face, tex, isTranslucent, isTorch);
        }
    }

    private void addQuad(Chunk chunk, int x, int y, int z, int worldY, int face, int tex, boolean isTranslucent, boolean isTorch) {
        ensureCapacity(4, 6, isTranslucent);

        int baseIndex = isTranslucent ? transVertexCount : opaqueVertexCount;

        // Coordinates:
        int x0 = x, x1 = x + 1;
        int y0 = y, y1 = y + 1;
        int z0 = z, z1 = z + 1;

        int ao0, ao1, ao2, ao3;
        int l0, l1, l2, l3;
        int p0, p1, p2, p3;

        if (isTorch) {
            ao0 = ao1 = ao2 = ao3 = 3;
            l0 = l1 = l2 = l3 = 15;
            switch (face) {
                case Block.FACE_UP -> {
                    p0 = pack(x0, y1, z1, face, tex, 3, 0, 15);
                    p1 = pack(x1, y1, z1, face, tex, 3, 1, 15);
                    p2 = pack(x1, y1, z0, face, tex, 3, 2, 15);
                    p3 = pack(x0, y1, z0, face, tex, 3, 3, 15);
                }
                case Block.FACE_DOWN -> {
                    p0 = pack(x0, y0, z0, face, tex, 3, 0, 15);
                    p1 = pack(x1, y0, z0, face, tex, 3, 1, 15);
                    p2 = pack(x1, y0, z1, face, tex, 3, 2, 15);
                    p3 = pack(x0, y0, z1, face, tex, 3, 3, 15);
                }
                case Block.FACE_NORTH -> {
                    p0 = pack(x1, y0, z0, face, tex, 3, 3, 15);
                    p1 = pack(x0, y0, z0, face, tex, 3, 2, 15);
                    p2 = pack(x0, y1, z0, face, tex, 3, 1, 15);
                    p3 = pack(x1, y1, z0, face, tex, 3, 0, 15);
                }
                case Block.FACE_SOUTH -> {
                    p0 = pack(x0, y0, z1, face, tex, 3, 3, 15);
                    p1 = pack(x1, y0, z1, face, tex, 3, 2, 15);
                    p2 = pack(x1, y1, z1, face, tex, 3, 1, 15);
                    p3 = pack(x0, y1, z1, face, tex, 3, 0, 15);
                }
                case Block.FACE_WEST -> {
                    p0 = pack(x0, y0, z0, face, tex, 3, 3, 15);
                    p1 = pack(x0, y0, z1, face, tex, 3, 2, 15);
                    p2 = pack(x0, y1, z1, face, tex, 3, 1, 15);
                    p3 = pack(x0, y1, z0, face, tex, 3, 0, 15);
                }
                case Block.FACE_EAST -> {
                    p0 = pack(x1, y0, z1, face, tex, 3, 3, 15);
                    p1 = pack(x1, y0, z0, face, tex, 3, 2, 15);
                    p2 = pack(x1, y1, z0, face, tex, 3, 1, 15);
                    p3 = pack(x1, y1, z1, face, tex, 3, 0, 15);
                }
                default -> { p0 = p1 = p2 = p3 = 0; }
            }
        } else {
            switch (face) {
                case Block.FACE_UP -> { // +Y (Normal = 0, 1, 0)
                    ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x - 1, worldY + 1, z + 1));
                    ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x + 1, worldY + 1, z + 1));
                    ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x + 1, worldY + 1, z - 1));
                    ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x - 1, worldY + 1, z - 1));

                    int lc = chunk.getLight(x, worldY + 1, z);
                    int ls0 = chunk.getLight(x - 1, worldY + 1, z);
                    int ls1 = chunk.getLight(x + 1, worldY + 1, z);
                    int lszp = chunk.getLight(x, worldY + 1, z + 1);
                    int lszm = chunk.getLight(x, worldY + 1, z - 1);

                    l0 = getSmoothLight(lc, ls0, lszp, chunk.getLight(x - 1, worldY + 1, z + 1));
                    l1 = getSmoothLight(lc, ls1, lszp, chunk.getLight(x + 1, worldY + 1, z + 1));
                    l2 = getSmoothLight(lc, ls1, lszm, chunk.getLight(x + 1, worldY + 1, z - 1));
                    l3 = getSmoothLight(lc, ls0, lszm, chunk.getLight(x - 1, worldY + 1, z - 1));

                    p0 = pack(x0, y1, z1, face, tex, ao0, 0, l0);
                    p1 = pack(x1, y1, z1, face, tex, ao1, 1, l1);
                    p2 = pack(x1, y1, z0, face, tex, ao2, 2, l2);
                    p3 = pack(x0, y1, z0, face, tex, ao3, 3, l3);
                }
                case Block.FACE_DOWN -> { // -Y (Normal = 0, -1, 0)
                    ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x - 1, worldY - 1, z - 1));
                    ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x + 1, worldY - 1, z - 1));
                    ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x + 1, worldY - 1, z + 1));
                    ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x - 1, worldY - 1, z + 1));

                    int lc = chunk.getLight(x, worldY - 1, z);
                    int ls0 = chunk.getLight(x - 1, worldY - 1, z);
                    int ls1 = chunk.getLight(x + 1, worldY - 1, z);
                    int lszm = chunk.getLight(x, worldY - 1, z - 1);
                    int lszp = chunk.getLight(x, worldY - 1, z + 1);

                    l0 = getSmoothLight(lc, ls0, lszm, chunk.getLight(x - 1, worldY - 1, z - 1));
                    l1 = getSmoothLight(lc, ls1, lszm, chunk.getLight(x + 1, worldY - 1, z - 1));
                    l2 = getSmoothLight(lc, ls1, lszp, chunk.getLight(x + 1, worldY - 1, z + 1));
                    l3 = getSmoothLight(lc, ls0, lszp, chunk.getLight(x - 1, worldY - 1, z + 1));

                    p0 = pack(x0, y0, z0, face, tex, ao0, 0, l0);
                    p1 = pack(x1, y0, z0, face, tex, ao1, 1, l1);
                    p2 = pack(x1, y0, z1, face, tex, ao2, 2, l2);
                    p3 = pack(x0, y0, z1, face, tex, ao3, 3, l3);
                }
                case Block.FACE_NORTH -> { // -Z (Normal = 0, 0, -1)
                    ao0 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x + 1, worldY - 1, z - 1));
                    ao1 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x, worldY - 1, z - 1), isSolid(chunk, x - 1, worldY - 1, z - 1));
                    ao2 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x - 1, worldY + 1, z - 1));
                    ao3 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x, worldY + 1, z - 1), isSolid(chunk, x + 1, worldY + 1, z - 1));

                    int lc = chunk.getLight(x, worldY, z - 1);
                    int lsx1 = chunk.getLight(x + 1, worldY, z - 1);
                    int lsx0 = chunk.getLight(x - 1, worldY, z - 1);
                    int lsym = chunk.getLight(x, worldY - 1, z - 1);
                    int lsyp = chunk.getLight(x, worldY + 1, z - 1);

                    l0 = getSmoothLight(lc, lsx1, lsym, chunk.getLight(x + 1, worldY - 1, z - 1));
                    l1 = getSmoothLight(lc, lsx0, lsym, chunk.getLight(x - 1, worldY - 1, z - 1));
                    l2 = getSmoothLight(lc, lsx0, lsyp, chunk.getLight(x - 1, worldY + 1, z - 1));
                    l3 = getSmoothLight(lc, lsx1, lsyp, chunk.getLight(x + 1, worldY + 1, z - 1));

                    p0 = pack(x1, y0, z0, face, tex, ao0, 3, l0);
                    p1 = pack(x0, y0, z0, face, tex, ao1, 2, l1);
                    p2 = pack(x0, y1, z0, face, tex, ao2, 1, l2);
                    p3 = pack(x1, y1, z0, face, tex, ao3, 0, l3);
                }
                case Block.FACE_SOUTH -> { // +Z (Normal = 0, 0, 1)
                    ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x - 1, worldY - 1, z + 1));
                    ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x, worldY - 1, z + 1), isSolid(chunk, x + 1, worldY - 1, z + 1));
                    ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x + 1, worldY + 1, z + 1));
                    ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x, worldY + 1, z + 1), isSolid(chunk, x - 1, worldY + 1, z + 1));

                    int lc = chunk.getLight(x, worldY, z + 1);
                    int lsx0 = chunk.getLight(x - 1, worldY, z + 1);
                    int lsx1 = chunk.getLight(x + 1, worldY, z + 1);
                    int lsym = chunk.getLight(x, worldY - 1, z + 1);
                    int lsyp = chunk.getLight(x, worldY + 1, z + 1);

                    l0 = getSmoothLight(lc, lsx0, lsym, chunk.getLight(x - 1, worldY - 1, z + 1));
                    l1 = getSmoothLight(lc, lsx1, lsym, chunk.getLight(x + 1, worldY - 1, z + 1));
                    l2 = getSmoothLight(lc, lsx1, lsyp, chunk.getLight(x + 1, worldY + 1, z + 1));
                    l3 = getSmoothLight(lc, lsx0, lsyp, chunk.getLight(x - 1, worldY + 1, z + 1));

                    p0 = pack(x0, y0, z1, face, tex, ao0, 3, l0);
                    p1 = pack(x1, y0, z1, face, tex, ao1, 2, l1);
                    p2 = pack(x1, y1, z1, face, tex, ao2, 1, l2);
                    p3 = pack(x0, y1, z1, face, tex, ao3, 0, l3);
                }
                case Block.FACE_WEST -> { // -X (Normal = -1, 0, 0)
                    ao0 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x - 1, worldY - 1, z - 1));
                    ao1 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x - 1, worldY - 1, z), isSolid(chunk, x - 1, worldY - 1, z + 1));
                    ao2 = getAO(chunk, isSolid(chunk, x - 1, worldY, z + 1), isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x - 1, worldY + 1, z + 1));
                    ao3 = getAO(chunk, isSolid(chunk, x - 1, worldY, z - 1), isSolid(chunk, x - 1, worldY + 1, z), isSolid(chunk, x - 1, worldY + 1, z - 1));

                    int lc = chunk.getLight(x - 1, worldY, z);
                    int lsz0 = chunk.getLight(x - 1, worldY, z - 1);
                    int lsz1 = chunk.getLight(x - 1, worldY, z + 1);
                    int lsym = chunk.getLight(x - 1, worldY - 1, z);
                    int lsyp = chunk.getLight(x - 1, worldY + 1, z);

                    l0 = getSmoothLight(lc, lsz0, lsym, chunk.getLight(x - 1, worldY - 1, z - 1));
                    l1 = getSmoothLight(lc, lsz1, lsym, chunk.getLight(x - 1, worldY - 1, z + 1));
                    l2 = getSmoothLight(lc, lsz1, lsyp, chunk.getLight(x - 1, worldY + 1, z + 1));
                    l3 = getSmoothLight(lc, lsz0, lsyp, chunk.getLight(x - 1, worldY + 1, z - 1));

                    p0 = pack(x0, y0, z0, face, tex, ao0, 3, l0);
                    p1 = pack(x0, y0, z1, face, tex, ao1, 2, l1);
                    p2 = pack(x0, y1, z1, face, tex, ao2, 1, l2);
                    p3 = pack(x0, y1, z0, face, tex, ao3, 0, l3);
                }
                case Block.FACE_EAST -> { // +X (Normal = +1, 0, 0)
                    ao0 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x + 1, worldY - 1, z + 1));
                    ao1 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x + 1, worldY - 1, z), isSolid(chunk, x + 1, worldY - 1, z - 1));
                    ao2 = getAO(chunk, isSolid(chunk, x + 1, worldY, z - 1), isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x + 1, worldY + 1, z - 1));
                    ao3 = getAO(chunk, isSolid(chunk, x + 1, worldY, z + 1), isSolid(chunk, x + 1, worldY + 1, z), isSolid(chunk, x + 1, worldY + 1, z + 1));

                    int lc = chunk.getLight(x + 1, worldY, z);
                    int lsz1 = chunk.getLight(x + 1, worldY, z + 1);
                    int lsz0 = chunk.getLight(x + 1, worldY, z - 1);
                    int lsym = chunk.getLight(x + 1, worldY - 1, z);
                    int lsyp = chunk.getLight(x + 1, worldY + 1, z);

                    l0 = getSmoothLight(lc, lsz1, lsym, chunk.getLight(x + 1, worldY - 1, z + 1));
                    l1 = getSmoothLight(lc, lsz0, lsym, chunk.getLight(x + 1, worldY - 1, z - 1));
                    l2 = getSmoothLight(lc, lsz0, lsyp, chunk.getLight(x + 1, worldY + 1, z - 1));
                    l3 = getSmoothLight(lc, lsz1, lsyp, chunk.getLight(x + 1, worldY + 1, z + 1));

                    p0 = pack(x1, y0, z1, face, tex, ao0, 3, l0);
                    p1 = pack(x1, y0, z0, face, tex, ao1, 2, l1);
                    p2 = pack(x1, y1, z0, face, tex, ao2, 1, l2);
                    p3 = pack(x1, y1, z1, face, tex, ao3, 0, l3);
                }
                default -> {
                    ao0 = 3; ao1 = 3; ao2 = 3; ao3 = 3;
                    l0 = 15; l1 = 15; l2 = 15; l3 = 15;
                    p0 = p1 = p2 = p3 = 0;
                }
            }
        }

        if (isTranslucent) {
            transVertices[transVertexCount++] = p0;
            transVertices[transVertexCount++] = p1;
            transVertices[transVertexCount++] = p2;
            transVertices[transVertexCount++] = p3;

            if (ao0 + ao2 > ao1 + ao3) {
                transIndices[transIndexCount++] = baseIndex;
                transIndices[transIndexCount++] = baseIndex + 1;
                transIndices[transIndexCount++] = baseIndex + 2;

                transIndices[transIndexCount++] = baseIndex;
                transIndices[transIndexCount++] = baseIndex + 2;
                transIndices[transIndexCount++] = baseIndex + 3;
            } else {
                transIndices[transIndexCount++] = baseIndex + 1;
                transIndices[transIndexCount++] = baseIndex + 2;
                transIndices[transIndexCount++] = baseIndex + 3;

                transIndices[transIndexCount++] = baseIndex + 1;
                transIndices[transIndexCount++] = baseIndex + 3;
                transIndices[transIndexCount++] = baseIndex;
            }
        } else {
            opaqueVertices[opaqueVertexCount++] = p0;
            opaqueVertices[opaqueVertexCount++] = p1;
            opaqueVertices[opaqueVertexCount++] = p2;
            opaqueVertices[opaqueVertexCount++] = p3;

            if (ao0 + ao2 > ao1 + ao3) {
                opaqueIndices[opaqueIndexCount++] = baseIndex;
                opaqueIndices[opaqueIndexCount++] = baseIndex + 1;
                opaqueIndices[opaqueIndexCount++] = baseIndex + 2;

                opaqueIndices[opaqueIndexCount++] = baseIndex;
                opaqueIndices[opaqueIndexCount++] = baseIndex + 2;
                opaqueIndices[opaqueIndexCount++] = baseIndex + 3;
            } else {
                opaqueIndices[opaqueIndexCount++] = baseIndex + 1;
                opaqueIndices[opaqueIndexCount++] = baseIndex + 2;
                opaqueIndices[opaqueIndexCount++] = baseIndex + 3;

                opaqueIndices[opaqueIndexCount++] = baseIndex + 1;
                opaqueIndices[opaqueIndexCount++] = baseIndex + 3;
                opaqueIndices[opaqueIndexCount++] = baseIndex;
            }
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

    private static int getSmoothLight(int lCenter, int lSide1, int lSide2, int lCorner) {
        return (lCenter + lSide1 + lSide2 + lCorner) / 4;
    }

    private static int pack(int x, int y, int z, int face, int tex, int ao, int uv, int light) {
        return (x & 0x1F) |
               ((y & 0x1F) << 5) |
               ((z & 0x1F) << 10) |
               ((face & 0x07) << 15) |
               ((tex & 0x1F) << 18) |
               ((ao & 0x03) << 23) |
               ((uv & 0x03) << 25) |
               ((light & 0x0F) << 27);
    }

    private void ensureCapacity(int additionalVerts, int additionalIndices, boolean isTranslucent) {
        if (isTranslucent) {
            if (transVertexCount + additionalVerts > transVertices.length) {
                int[] n = new int[transVertices.length * 2];
                System.arraycopy(transVertices, 0, n, 0, transVertexCount);
                transVertices = n;
            }
            if (transIndexCount + additionalIndices > transIndices.length) {
                int[] n = new int[transIndices.length * 2];
                System.arraycopy(transIndices, 0, n, 0, transIndexCount);
                transIndices = n;
            }
        } else {
            if (opaqueVertexCount + additionalVerts > opaqueVertices.length) {
                int[] n = new int[opaqueVertices.length * 2];
                System.arraycopy(opaqueVertices, 0, n, 0, opaqueVertexCount);
                opaqueVertices = n;
            }
            if (opaqueIndexCount + additionalIndices > opaqueIndices.length) {
                int[] n = new int[opaqueIndices.length * 2];
                System.arraycopy(opaqueIndices, 0, n, 0, opaqueIndexCount);
                opaqueIndices = n;
            }
        }
    }
}
