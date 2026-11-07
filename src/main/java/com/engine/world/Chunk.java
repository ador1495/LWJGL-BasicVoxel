package com.engine.world;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int SECTIONS_COUNT = CHUNK_HEIGHT / CHUNK_SIZE; // 16 sections

    private final int chunkX;
    private final int chunkZ;
    private final ChunkSection[] sections = new ChunkSection[SECTIONS_COUNT];

    // Direct neighbor references for zero-overhead boundary meshing
    public Chunk north; // Z - 1
    public Chunk south; // Z + 1
    public Chunk west;  // X - 1
    public Chunk east;  // X + 1

    private boolean generated = false;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        for (int y = 0; y < SECTIONS_COUNT; y++) {
            sections[y] = new ChunkSection(chunkX, y, chunkZ);
        }
    }

    public byte getBlock(int localX, int worldY, int localZ) {
        if (worldY < 0 || worldY >= CHUNK_HEIGHT) {
            return 0; // Air outside vertical boundaries
        }

        // Fast boundary routing to neighbors
        if (localX < 0) {
            return (west != null) ? west.getBlock(localX + CHUNK_SIZE, worldY, localZ) : 0;
        }
        if (localX >= CHUNK_SIZE) {
            return (east != null) ? east.getBlock(localX - CHUNK_SIZE, worldY, localZ) : 0;
        }
        if (localZ < 0) {
            return (north != null) ? north.getBlock(localX, worldY, localZ + CHUNK_SIZE) : 0;
        }
        if (localZ >= CHUNK_SIZE) {
            return (south != null) ? south.getBlock(localX, worldY, localZ - CHUNK_SIZE) : 0;
        }

        int sectionIdx = worldY >> 4;
        int localY = worldY & 15;
        return sections[sectionIdx].getBlock(localX, localY, localZ);
    }

    public void setBlock(int localX, int worldY, int localZ, byte blockId) {
        if (worldY < 0 || worldY >= CHUNK_HEIGHT || localX < 0 || localX >= CHUNK_SIZE || localZ < 0 || localZ >= CHUNK_SIZE) {
            return;
        }

        int sectionIdx = worldY >> 4;
        int localY = worldY & 15;
        sections[sectionIdx].setBlock(localX, localY, localZ, blockId);

        // Mark neighboring sections dirty if block is on a boundary
        if (localY == 0 && sectionIdx > 0) {
            sections[sectionIdx - 1].setDirty(true);
        } else if (localY == 15 && sectionIdx < SECTIONS_COUNT - 1) {
            sections[sectionIdx + 1].setDirty(true);
        }

        if (localX == 0 && west != null) {
            west.getSection(sectionIdx).setDirty(true);
        } else if (localX == 15 && east != null) {
            east.getSection(sectionIdx).setDirty(true);
        }

        if (localZ == 0 && north != null) {
            north.getSection(sectionIdx).setDirty(true);
        } else if (localZ == 15 && south != null) {
            south.getSection(sectionIdx).setDirty(true);
        }
    }

    public ChunkSection getSection(int sectionY) {
        if (sectionY < 0 || sectionY >= SECTIONS_COUNT) return null;
        return sections[sectionY];
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getWorldX() {
        return chunkX * CHUNK_SIZE;
    }

    public int getWorldZ() {
        return chunkZ * CHUNK_SIZE;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public void cleanup() {
        for (ChunkSection section : sections) {
            section.cleanup();
        }
    }
}

