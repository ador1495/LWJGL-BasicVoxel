package com.engine.world;

import com.engine.gfx.TextureGenerator;

public enum Block {
    AIR((byte) 0, "Air", false, true, false, 0, 0, 0),
    STONE((byte) 1, "Stone", true, false, false, TextureGenerator.TEX_STONE, TextureGenerator.TEX_STONE, TextureGenerator.TEX_STONE),
    DIRT((byte) 2, "Dirt", true, false, false, TextureGenerator.TEX_DIRT, TextureGenerator.TEX_DIRT, TextureGenerator.TEX_DIRT),
    GRASS((byte) 3, "Grass", true, false, false, TextureGenerator.TEX_GRASS_TOP, TextureGenerator.TEX_DIRT, TextureGenerator.TEX_GRASS_SIDE),
    COBBLESTONE((byte) 4, "Cobblestone", true, false, false, TextureGenerator.TEX_COBBLESTONE, TextureGenerator.TEX_COBBLESTONE, TextureGenerator.TEX_COBBLESTONE),
    WOOD((byte) 5, "Wood", true, false, false, TextureGenerator.TEX_WOOD_TOP, TextureGenerator.TEX_WOOD_TOP, TextureGenerator.TEX_WOOD_SIDE),
    LEAVES((byte) 6, "Leaves", true, true, false, TextureGenerator.TEX_LEAVES, TextureGenerator.TEX_LEAVES, TextureGenerator.TEX_LEAVES),
    SAND((byte) 7, "Sand", true, false, false, TextureGenerator.TEX_SAND, TextureGenerator.TEX_SAND, TextureGenerator.TEX_SAND),
    WATER((byte) 8, "Water", false, true, true, TextureGenerator.TEX_WATER, TextureGenerator.TEX_WATER, TextureGenerator.TEX_WATER),
    BEDROCK((byte) 9, "Bedrock", true, false, false, TextureGenerator.TEX_BEDROCK, TextureGenerator.TEX_BEDROCK, TextureGenerator.TEX_BEDROCK),
    GLASS((byte) 10, "Glass", true, true, false, TextureGenerator.TEX_GLASS, TextureGenerator.TEX_GLASS, TextureGenerator.TEX_GLASS),
    BRICK((byte) 11, "Brick", true, false, false, TextureGenerator.TEX_BRICK, TextureGenerator.TEX_BRICK, TextureGenerator.TEX_BRICK),
    PLANKS((byte) 12, "Planks", true, false, false, TextureGenerator.TEX_PLANKS, TextureGenerator.TEX_PLANKS, TextureGenerator.TEX_PLANKS),
    COAL_ORE((byte) 13, "Coal Ore", true, false, false, TextureGenerator.TEX_COAL_ORE, TextureGenerator.TEX_COAL_ORE, TextureGenerator.TEX_COAL_ORE),
    IRON_ORE((byte) 14, "Iron Ore", true, false, false, TextureGenerator.TEX_IRON_ORE, TextureGenerator.TEX_IRON_ORE, TextureGenerator.TEX_IRON_ORE),
    GOLD_ORE((byte) 15, "Gold Ore", true, false, false, TextureGenerator.TEX_GOLD_ORE, TextureGenerator.TEX_GOLD_ORE, TextureGenerator.TEX_GOLD_ORE),
    DIAMOND_ORE((byte) 16, "Diamond Ore", true, false, false, TextureGenerator.TEX_DIAMOND_ORE, TextureGenerator.TEX_DIAMOND_ORE, TextureGenerator.TEX_DIAMOND_ORE),
    GLOWSTONE((byte) 17, "Glowstone", true, false, false, TextureGenerator.TEX_GLOWSTONE, TextureGenerator.TEX_GLOWSTONE, TextureGenerator.TEX_GLOWSTONE),
    SNOW((byte) 18, "Snow", true, false, false, TextureGenerator.TEX_SNOW, TextureGenerator.TEX_DIRT, TextureGenerator.TEX_SNOW);

    public static final int FACE_UP = 0;    // +Y
    public static final int FACE_DOWN = 1;  // -Y
    public static final int FACE_NORTH = 2; // -Z
    public static final int FACE_SOUTH = 3; // +Z
    public static final int FACE_WEST = 4;  // -X
    public static final int FACE_EAST = 5;  // +X

    public final byte id;
    public final String name;
    public final boolean solid;
    public final boolean transparent;
    public final boolean liquid;
    private final int topTex;
    private final int bottomTex;
    private final int sideTex;

    private static final Block[] BY_ID = new Block[256];

    static {
        for (Block b : values()) {
            BY_ID[b.id & 0xFF] = b;
        }
    }

    Block(byte id, String name, boolean solid, boolean transparent, boolean liquid, int topTex, int bottomTex, int sideTex) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.transparent = transparent;
        this.liquid = liquid;
        this.topTex = topTex;
        this.bottomTex = bottomTex;
        this.sideTex = sideTex;
    }

    public static Block getById(byte id) {
        Block b = BY_ID[id & 0xFF];
        return (b != null) ? b : AIR;
    }

    public int getTexture(int face) {
        if (face == FACE_UP) return topTex;
        if (face == FACE_DOWN) return bottomTex;
        return sideTex;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isLiquid() {
        return liquid;
    }
}

