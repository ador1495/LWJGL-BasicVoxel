package com.engine.world;

import java.util.Random;

public class WorldGenerator {
    private final Noise heightNoise;
    private final Noise caveNoise;
    private final Noise detailNoise;
    private final long seed;

    public static final int SEA_LEVEL = 62;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.heightNoise = new Noise(seed);
        this.caveNoise = new Noise(seed + 101);
        this.detailNoise = new Noise(seed + 202);
    }

    public void generateChunk(Chunk chunk) {
        int chunkWorldX = chunk.getWorldX();
        int chunkWorldZ = chunk.getWorldZ();

        int[][] heightmap = new int[Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE];

        // 1. Calculate terrain heightmap
        for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                double worldX = chunkWorldX + x;
                double worldZ = chunkWorldZ + z;

                // Multi-octave terrain height
                double n = heightNoise.octaveNoise2D(worldX * 0.005, worldZ * 0.005, 4, 0.5, 2.0);
                double d = detailNoise.noise2D(worldX * 0.02, worldZ * 0.02);

                int height = (int) (64 + (n * 28.0) + (d * 6.0));
                if (height < 10) height = 10;
                if (height >= Chunk.CHUNK_HEIGHT - 10) height = Chunk.CHUNK_HEIGHT - 11;
                heightmap[x][z] = height;

                // Bedrock layer
                chunk.setBlock(x, 0, z, Block.BEDROCK.id);

                // Fill layers
                for (int y = 1; y <= height; y++) {
                    // Cave carving using 3D noise
                    if (y > 8 && y < height - 4) {
                        double cave = caveNoise.noise3D(worldX * 0.04, y * 0.05, worldZ * 0.04);
                        if (cave > 0.38) {
                            continue; // Carve cave (leave AIR)
                        }
                    }

                    if (y == height) {
                        if (height <= SEA_LEVEL + 1) {
                            chunk.setBlock(x, y, z, Block.SAND.id);
                        } else {
                            chunk.setBlock(x, y, z, Block.GRASS.id);
                        }
                    } else if (y >= height - 3) {
                        if (height <= SEA_LEVEL + 1) {
                            chunk.setBlock(x, y, z, Block.SAND.id);
                        } else {
                            chunk.setBlock(x, y, z, Block.DIRT.id);
                        }
                    } else {
                        chunk.setBlock(x, y, z, Block.STONE.id);
                    }
                }

                // Water filling up to sea level
                if (height < SEA_LEVEL) {
                    for (int y = height + 1; y <= SEA_LEVEL; y++) {
                        chunk.setBlock(x, y, z, Block.WATER.id);
                    }
                }
            }
        }

        // 2. Procedural Trees
        Random rand = new Random(seed ^ ((long) chunk.getChunkX() << 32) ^ (long) chunk.getChunkZ());
        for (int z = 2; z < Chunk.CHUNK_SIZE - 2; z++) {
            for (int x = 2; x < Chunk.CHUNK_SIZE - 2; x++) {
                int height = heightmap[x][z];
                if (height > SEA_LEVEL + 1 && chunk.getBlock(x, height, z) == Block.GRASS.id) {
                    if (rand.nextFloat() < 0.025f) { // Tree density
                        generateTree(chunk, x, height + 1, z, rand);
                    }
                }
            }
        }

        chunk.setGenerated(true);
    }

    private void generateTree(Chunk chunk, int x, int startY, int z, Random rand) {
        int trunkHeight = 4 + rand.nextInt(3); // 4-6 blocks

        // Leaves canopy
        int leavesBottom = startY + trunkHeight - 2;
        int leavesTop = startY + trunkHeight + 1;

        for (int y = leavesBottom; y <= leavesTop; y++) {
            int radius = (y >= startY + trunkHeight) ? 1 : 2;
            for (int lx = -radius; lx <= radius; lx++) {
                for (int lz = -radius; lz <= radius; lz++) {
                    if (Math.abs(lx) == radius && Math.abs(lz) == radius && (y == leavesTop || rand.nextFloat() < 0.3f)) {
                        continue; // Round out leaves corners
                    }
                    int px = x + lx;
                    int pz = z + lz;
                    if (chunk.getBlock(px, y, pz) == 0) {
                        chunk.setBlock(px, y, pz, Block.LEAVES.id);
                    }
                }
            }
        }

        // Wood trunk
        for (int y = 0; y < trunkHeight; y++) {
            chunk.setBlock(x, startY + y, z, Block.WOOD.id);
        }
    }
}

