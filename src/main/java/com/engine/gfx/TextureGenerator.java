package com.engine.gfx;

import java.nio.ByteBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;

public class TextureGenerator {
    public static final int TEXTURE_SIZE = 16;
    public static final int NUM_TEXTURES = 20;

    public static final int TEX_DIRT = 0;
    public static final int TEX_GRASS_TOP = 1;
    public static final int TEX_GRASS_SIDE = 2;
    public static final int TEX_STONE = 3;
    public static final int TEX_COBBLESTONE = 4;
    public static final int TEX_WOOD_SIDE = 5;
    public static final int TEX_WOOD_TOP = 6;
    public static final int TEX_LEAVES = 7;
    public static final int TEX_SAND = 8;
    public static final int TEX_WATER = 9;
    public static final int TEX_BEDROCK = 10;
    public static final int TEX_GLASS = 11;
    public static final int TEX_BRICK = 12;
    public static final int TEX_PLANKS = 13;
    public static final int TEX_COAL_ORE = 14;
    public static final int TEX_IRON_ORE = 15;
    public static final int TEX_GOLD_ORE = 16;
    public static final int TEX_DIAMOND_ORE = 17;
    public static final int TEX_GLOWSTONE = 18;
    public static final int TEX_SNOW = 19;

    public static ByteBuffer generateTextureArray() {
        int bytesPerLayer = TEXTURE_SIZE * TEXTURE_SIZE * 4;
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytesPerLayer * NUM_TEXTURES);

        for (int layer = 0; layer < NUM_TEXTURES; layer++) {
            int[] pixels = generateLayer(layer);
            for (int pixel : pixels) {
                // RGBA format
                byte r = (byte) ((pixel >> 24) & 0xFF);
                byte g = (byte) ((pixel >> 16) & 0xFF);
                byte b = (byte) ((pixel >> 8) & 0xFF);
                byte a = (byte) (pixel & 0xFF);
                buffer.put(r).put(g).put(b).put(a);
            }
        }
        buffer.flip();
        return buffer;
    }

    private static int[] generateLayer(int layer) {
        int[] pixels = new int[TEXTURE_SIZE * TEXTURE_SIZE];
        Random rand = new Random(layer * 1337L + 42L);

        switch (layer) {
            case TEX_DIRT -> makeDirt(pixels, rand);
            case TEX_GRASS_TOP -> makeGrassTop(pixels, rand);
            case TEX_GRASS_SIDE -> makeGrassSide(pixels, rand);
            case TEX_STONE -> makeStone(pixels, rand);
            case TEX_COBBLESTONE -> makeCobblestone(pixels, rand);
            case TEX_WOOD_SIDE -> makeWoodSide(pixels, rand);
            case TEX_WOOD_TOP -> makeWoodTop(pixels, rand);
            case TEX_LEAVES -> makeLeaves(pixels, rand);
            case TEX_SAND -> makeSand(pixels, rand);
            case TEX_WATER -> makeWater(pixels, rand);
            case TEX_BEDROCK -> makeBedrock(pixels, rand);
            case TEX_GLASS -> makeGlass(pixels, rand);
            case TEX_BRICK -> makeBrick(pixels, rand);
            case TEX_PLANKS -> makePlanks(pixels, rand);
            case TEX_COAL_ORE -> makeOre(pixels, rand, new int[]{rgba(25, 25, 25, 255), rgba(40, 40, 40, 255)});
            case TEX_IRON_ORE -> makeOre(pixels, rand, new int[]{rgba(216, 175, 147, 255), rgba(188, 142, 113, 255)});
            case TEX_GOLD_ORE -> makeOre(pixels, rand, new int[]{rgba(255, 223, 70, 255), rgba(230, 190, 40, 255)});
            case TEX_DIAMOND_ORE -> makeOre(pixels, rand, new int[]{rgba(92, 240, 240, 255), rgba(45, 205, 215, 255)});
            case TEX_GLOWSTONE -> makeGlowstone(pixels, rand);
            case TEX_SNOW -> makeSnow(pixels, rand);
            default -> {
                for (int i = 0; i < pixels.length; i++) pixels[i] = rgba(255, 0, 255, 255);
            }
        }
        return pixels;
    }

    private static int rgba(int r, int g, int b, int a) {
        return ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF);
    }

    private static void makeDirt(int[] px, Random rand) {
        int[] palette = {
            rgba(134, 96, 67, 255),
            rgba(121, 85, 58, 255),
            rgba(104, 75, 52, 255),
            rgba(87, 61, 38, 255),
            rgba(156, 115, 78, 255)
        };
        for (int i = 0; i < px.length; i++) {
            px[i] = palette[rand.nextInt(palette.length)];
        }
    }

    private static void makeGrassTop(int[] px, Random rand) {
        int[] palette = {
            rgba(76, 138, 48, 255),
            rgba(92, 160, 56, 255),
            rgba(62, 114, 38, 255),
            rgba(85, 148, 50, 255),
            rgba(105, 175, 65, 255)
        };
        for (int i = 0; i < px.length; i++) {
            px[i] = palette[rand.nextInt(palette.length)];
        }
    }

    private static void makeGrassSide(int[] px, Random rand) {
        makeDirt(px, rand);
        int[] grassPalette = {
            rgba(76, 138, 48, 255),
            rgba(92, 160, 56, 255),
            rgba(62, 114, 38, 255),
            rgba(85, 148, 50, 255)
        };
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            int depth = 2 + (x % 3 == 0 ? 1 : 0) + (x % 5 == 0 ? 1 : 0);
            for (int y = 0; y < depth; y++) {
                px[y * TEXTURE_SIZE + x] = grassPalette[rand.nextInt(grassPalette.length)];
            }
        }
    }

    private static void makeStone(int[] px, Random rand) {
        int[] palette = {
            rgba(115, 115, 115, 255),
            rgba(128, 128, 128, 255),
            rgba(96, 96, 96, 255),
            rgba(142, 142, 142, 255),
            rgba(105, 105, 105, 255)
        };
        for (int i = 0; i < px.length; i++) {
            px[i] = palette[rand.nextInt(palette.length)];
        }
    }

    private static void makeCobblestone(int[] px, Random rand) {
        makeStone(px, rand);
        int dark = rgba(50, 50, 50, 255);
        int light = rgba(150, 150, 150, 255);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if ((x == 4 || x == 11) && y > 2 && y < 14) px[y * TEXTURE_SIZE + x] = dark;
                if ((y == 4 || y == 11) && x > 2 && x < 14) px[y * TEXTURE_SIZE + x] = dark;
                if (rand.nextFloat() < 0.08f) px[y * TEXTURE_SIZE + x] = light;
            }
        }
    }

    private static void makeWoodSide(int[] px, Random rand) {
        int darkBark = rgba(60, 43, 20, 255);
        int midBark = rgba(104, 83, 50, 255);
        int lightBark = rgba(118, 95, 59, 255);
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            boolean groove = (x == 3 || x == 8 || x == 13);
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                if (groove || rand.nextFloat() < 0.15f) {
                    px[y * TEXTURE_SIZE + x] = darkBark;
                } else if (rand.nextFloat() < 0.4f) {
                    px[y * TEXTURE_SIZE + x] = lightBark;
                } else {
                    px[y * TEXTURE_SIZE + x] = midBark;
                }
            }
        }
    }

    private static void makeWoodTop(int[] px, Random rand) {
        int bark = rgba(60, 43, 20, 255);
        int ring1 = rgba(140, 112, 64, 255);
        int ring2 = rgba(179, 146, 93, 255);
        int center = rgba(104, 83, 50, 255);

        float cx = 7.5f;
        float cy = 7.5f;
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                float dist = (float) Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                if (dist > 7.0f) {
                    px[y * TEXTURE_SIZE + x] = bark;
                } else if (dist > 4.5f) {
                    px[y * TEXTURE_SIZE + x] = ((x + y) % 2 == 0) ? ring1 : ring2;
                } else if (dist > 2.0f) {
                    px[y * TEXTURE_SIZE + x] = ring2;
                } else {
                    px[y * TEXTURE_SIZE + x] = center;
                }
            }
        }
    }

    private static void makeLeaves(int[] px, Random rand) {
        int darkGreen = rgba(38, 95, 20, 255);
        int midGreen = rgba(55, 125, 30, 255);
        int lightGreen = rgba(75, 155, 42, 255);
        int transparent = rgba(0, 0, 0, 0);

        for (int i = 0; i < px.length; i++) {
            float r = rand.nextFloat();
            if (r < 0.25f) {
                px[i] = transparent;
            } else if (r < 0.55f) {
                px[i] = midGreen;
            } else if (r < 0.85f) {
                px[i] = darkGreen;
            } else {
                px[i] = lightGreen;
            }
        }
    }

    private static void makeSand(int[] px, Random rand) {
        int[] palette = {
            rgba(219, 207, 163, 255),
            rgba(208, 195, 147, 255),
            rgba(226, 217, 179, 255),
            rgba(194, 181, 130, 255)
        };
        for (int i = 0; i < px.length; i++) {
            px[i] = palette[rand.nextInt(palette.length)];
        }
    }

    private static void makeWater(int[] px, Random rand) {
        int baseWater = rgba(46, 111, 191, 190);
        int lightWave = rgba(70, 140, 215, 190);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if ((x + y) % 4 == 0 || rand.nextFloat() < 0.1f) {
                    px[y * TEXTURE_SIZE + x] = lightWave;
                } else {
                    px[y * TEXTURE_SIZE + x] = baseWater;
                }
            }
        }
    }

    private static void makeBedrock(int[] px, Random rand) {
        int[] palette = {
            rgba(20, 20, 20, 255),
            rgba(40, 40, 40, 255),
            rgba(10, 10, 10, 255),
            rgba(65, 65, 65, 255),
            rgba(85, 85, 85, 255)
        };
        for (int i = 0; i < px.length; i++) {
            px[i] = palette[rand.nextInt(palette.length)];
        }
    }

    private static void makeGlass(int[] px, Random rand) {
        int frame = rgba(220, 240, 255, 255);
        int glint = rgba(255, 255, 255, 160);
        int transparent = rgba(0, 0, 0, 0);

        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if (x == 0 || x == 15 || y == 0 || y == 15) {
                    px[y * TEXTURE_SIZE + x] = frame;
                } else if ((x == y || x == y - 1) && x > 2 && x < 7) {
                    px[y * TEXTURE_SIZE + x] = glint;
                } else if ((x == y + 6) && x > 9 && x < 13) {
                    px[y * TEXTURE_SIZE + x] = glint;
                } else {
                    px[y * TEXTURE_SIZE + x] = transparent;
                }
            }
        }
    }

    private static void makeBrick(int[] px, Random rand) {
        int mortar = rgba(190, 180, 170, 255);
        int brick1 = rgba(170, 60, 45, 255);
        int brick2 = rgba(150, 50, 35, 255);
        int brick3 = rgba(190, 75, 55, 255);

        for (int y = 0; y < TEXTURE_SIZE; y++) {
            boolean isMortarY = (y % 4 == 3);
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int row = y / 4;
                int shift = (row % 2 == 0) ? 0 : 4;
                boolean isMortarX = ((x + shift) % 8 == 7);

                if (isMortarY || isMortarX) {
                    px[y * TEXTURE_SIZE + x] = mortar;
                } else {
                    float r = rand.nextFloat();
                    px[y * TEXTURE_SIZE + x] = (r < 0.4f) ? brick1 : (r < 0.8f ? brick2 : brick3);
                }
            }
        }
    }

    private static void makePlanks(int[] px, Random rand) {
        int darkLine = rgba(110, 80, 45, 255);
        int wood1 = rgba(175, 135, 85, 255);
        int wood2 = rgba(160, 120, 75, 255);
        int wood3 = rgba(190, 150, 100, 255);

        for (int y = 0; y < TEXTURE_SIZE; y++) {
            boolean isDivider = (y % 4 == 3);
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int plankRow = y / 4;
                int seamX = (plankRow % 2 == 0) ? 7 : 13;
                if (isDivider || x == seamX) {
                    px[y * TEXTURE_SIZE + x] = darkLine;
                } else {
                    float r = rand.nextFloat();
                    px[y * TEXTURE_SIZE + x] = (r < 0.5f) ? wood1 : (r < 0.85f ? wood2 : wood3);
                }
            }
        }
    }

    private static void makeOre(int[] px, Random rand, int[] oreColors) {
        makeStone(px, rand);
        // Cluster ore spots
        for (int cluster = 0; cluster < 3; cluster++) {
            int cx = 3 + rand.nextInt(10);
            int cy = 3 + rand.nextInt(10);
            int size = 1 + rand.nextInt(3);
            for (int dx = -size; dx <= size; dx++) {
                for (int dy = -size; dy <= size; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) <= size && rand.nextFloat() < 0.75f) {
                        int nx = cx + dx;
                        int ny = cy + dy;
                        if (nx >= 0 && nx < TEXTURE_SIZE && ny >= 0 && ny < TEXTURE_SIZE) {
                            px[ny * TEXTURE_SIZE + nx] = oreColors[rand.nextInt(oreColors.length)];
                        }
                    }
                }
            }
        }
    }

    private static void makeGlowstone(int[] px, Random rand) {
        int[] palette = {
            rgba(255, 220, 110, 255),
            rgba(240, 195, 80, 255),
            rgba(220, 165, 50, 255),
            rgba(180, 120, 30, 255),
            rgba(255, 245, 180, 255)
        };
        for (int i = 0; i < px.length; i++) {
            px[i] = palette[rand.nextInt(palette.length)];
        }
    }

    private static void makeSnow(int[] px, Random rand) {
        int[] palette = {
            rgba(245, 250, 255, 255),
            rgba(235, 245, 255, 255),
            rgba(225, 238, 250, 255),
            rgba(250, 252, 255, 255)
        };
        for (int i = 0; i < px.length; i++) {
            px[i] = palette[rand.nextInt(palette.length)];
        }
    }
}
