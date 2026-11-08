package com.engine.world;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int SECTIONS_COUNT = CHUNK_HEIGHT / CHUNK_SIZE; // 16 sections

    private final int chunkX;
    private final int chunkZ;
    private final ChunkSection[] sections = new ChunkSection[SECTIONS_COUNT];
    private final short[] heightmap = new short[CHUNK_SIZE * CHUNK_SIZE];

    // Direct neighbor references for boundary meshing
    public Chunk north; // Z - 1
    public Chunk south; // Z + 1
    public Chunk west;  // X - 1
    public Chunk east;  // X + 1

    private boolean generated = false;
    private World world;

    public void setWorld(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

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

        // Boundary routing to neighbors
        if (localX < 0) {
            return (west != null) ? west.getBlock(localX + CHUNK_SIZE, worldY, localZ) : (byte) (worldY <= WorldGenerator.SEA_LEVEL ? Block.WATER.id : 0);
        }
        if (localX >= CHUNK_SIZE) {
            return (east != null) ? east.getBlock(localX - CHUNK_SIZE, worldY, localZ) : (byte) (worldY <= WorldGenerator.SEA_LEVEL ? Block.WATER.id : 0);
        }
        if (localZ < 0) {
            return (north != null) ? north.getBlock(localX, worldY, localZ + CHUNK_SIZE) : (byte) (worldY <= WorldGenerator.SEA_LEVEL ? Block.WATER.id : 0);
        }
        if (localZ >= CHUNK_SIZE) {
            return (south != null) ? south.getBlock(localX, worldY, localZ - CHUNK_SIZE) : (byte) (worldY <= WorldGenerator.SEA_LEVEL ? Block.WATER.id : 0);
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

        // Update column heightmap
        int mapIdx = localZ * CHUNK_SIZE + localX;
        if (blockId != 0 && !Block.getById(blockId).transparent) {
            if (worldY > heightmap[mapIdx]) {
                heightmap[mapIdx] = (short) worldY;
            }
        } else if (worldY == heightmap[mapIdx]) {
            // Recalculate column top block
            short newH = 0;
            for (int y = worldY - 1; y >= 0; y--) {
                byte b = getBlock(localX, y, localZ);
                if (b != 0 && !Block.getById(b).transparent) {
                    newH = (short) y;
                    break;
                }
            }
            heightmap[mapIdx] = newH;
        }

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

    public boolean isBlockSunlit(int localX, int worldY, int localZ) {
        if (worldY >= CHUNK_HEIGHT) return true;
        int h = getHeight(localX, localZ);
        return worldY >= h;
    }

    public int getLight(int localX, int worldY, int localZ) {
        if (worldY >= CHUNK_HEIGHT) return 15;
        if (worldY < 0) return 0;

        int h = getHeight(localX, localZ);
        if (worldY >= h) {
            return 15; // Directly in open sunlight
        }

        // Horizontal sky light diffusion into cave opening / overhang
        int skyLight = 1; // Base cave ambient light
        for (int r = 1; r <= 6; r++) {
            int candidate = 15 - r * 2;
            if (candidate <= skyLight) break;

            if (worldY >= getHeight(localX + r, localZ) ||
                worldY >= getHeight(localX - r, localZ) ||
                worldY >= getHeight(localX, localZ + r) ||
                worldY >= getHeight(localX, localZ - r)) {
                skyLight = candidate;
                break;
            }
        }

        // Placed torch light
        int torchLight = (world != null) ? world.getTorchLightAt(chunkX * CHUNK_SIZE + localX, worldY, chunkZ * CHUNK_SIZE + localZ) : 0;
        return Math.max(skyLight, torchLight);
    }

    public int getHeight(int localX, int localZ) {
        if (localX < 0 || localX >= CHUNK_SIZE || localZ < 0 || localZ >= CHUNK_SIZE) {
            if (localX < 0 && west != null) return west.getHeight(localX + CHUNK_SIZE, localZ);
            if (localX >= CHUNK_SIZE && east != null) return east.getHeight(localX - CHUNK_SIZE, localZ);
            if (localZ < 0 && north != null) return north.getHeight(localX, localZ + CHUNK_SIZE);
            if (localZ >= CHUNK_SIZE && south != null) return south.getHeight(localX, localZ - CHUNK_SIZE);
            return WorldGenerator.SEA_LEVEL;
        }
        return heightmap[localZ * CHUNK_SIZE + localX];
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

    public boolean isFullyWired() {
        return north != null && south != null && west != null && east != null
            && north.generated && south.generated && west.generated && east.generated;
    }

    public void cleanup() {
        for (ChunkSection section : sections) {
            section.cleanup();
        }
    }
}
