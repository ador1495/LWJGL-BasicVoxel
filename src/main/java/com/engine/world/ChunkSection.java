package com.engine.world;

import com.engine.gfx.Mesh;
import com.engine.util.AABB;

public class ChunkSection {
    public static final int SIZE = 16;
    public static final int VOLUME = SIZE * SIZE * SIZE; // 4096

    private final byte[] blocks = new byte[VOLUME];
    private int nonAirCount = 0;
    private final int sectionX;
    private final int sectionY;
    private final int sectionZ;

    private final Mesh opaqueMesh;
    private final Mesh translucentMesh;
    private volatile boolean dirty = true;
    private volatile boolean building = false;
    private final AABB aabb;

    public ChunkSection(int sectionX, int sectionY, int sectionZ) {
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;

        int worldX = sectionX * SIZE;
        int worldY = sectionY * SIZE;
        int worldZ = sectionZ * SIZE;
        this.aabb = new AABB(worldX, worldY, worldZ, worldX + SIZE, worldY + SIZE, worldZ + SIZE);
        this.opaqueMesh = new Mesh();
        this.translucentMesh = new Mesh();
    }

    public static int getIndex(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) {
            return 0;
        }
        return blocks[(y << 8) | (z << 4) | x];
    }

    public void setBlock(int x, int y, int z, byte blockId) {
        int index = (y << 8) | (z << 4) | x;
        byte old = blocks[index];
        if (old == blockId) return;

        blocks[index] = blockId;

        if (old == 0 && blockId != 0) {
            nonAirCount++;
        } else if (old != 0 && blockId == 0) {
            nonAirCount--;
        }

        dirty = true;
    }

    public boolean isEmpty() {
        return nonAirCount == 0;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isBuilding() {
        return building;
    }

    public void setBuilding(boolean building) {
        this.building = building;
    }

    public Mesh getMesh() {
        return opaqueMesh;
    }

    public Mesh getOpaqueMesh() {
        return opaqueMesh;
    }

    public Mesh getTranslucentMesh() {
        return translucentMesh;
    }

    public boolean hasMesh() {
        return !opaqueMesh.isEmpty() || !translucentMesh.isEmpty();
    }

    public int getSectionX() {
        return sectionX;
    }

    public int getSectionY() {
        return sectionY;
    }

    public int getSectionZ() {
        return sectionZ;
    }

    public int getWorldX() {
        return sectionX * SIZE;
    }

    public int getWorldY() {
        return sectionY * SIZE;
    }

    public int getWorldZ() {
        return sectionZ * SIZE;
    }

    public AABB getAABB() {
        return aabb;
    }

    public void cleanup() {
        if (opaqueMesh != null) {
            opaqueMesh.cleanup();
        }
        if (translucentMesh != null) {
            translucentMesh.cleanup();
        }
    }
}

