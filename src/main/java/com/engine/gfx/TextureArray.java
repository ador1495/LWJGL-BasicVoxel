package com.engine.gfx;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public class TextureArray {
    private final int textureId;
    private final int width;
    private final int height;
    private final int layers;

    public TextureArray() {
        this.width = TextureGenerator.TEXTURE_SIZE;
        this.height = TextureGenerator.TEXTURE_SIZE;
        this.layers = TextureGenerator.NUM_TEXTURES;

        textureId = GL11.glGenTextures();
        bind();

        ByteBuffer buffer = TextureGenerator.generateTextureArray();

        // Target: GL_TEXTURE_2D_ARRAY (from GL30)
        GL12.glTexImage3D(
            GL30.GL_TEXTURE_2D_ARRAY,
            0,
            GL11.GL_RGBA8,
            width,
            height,
            layers,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            buffer
        );

        // Nearest filtering for authentic pixel-art voxels without blurring
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);

        unbind();
    }

    public void bind() {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureId);
    }

    public void bindToUnit(int unit) {
        GL30.glActiveTexture(GL30.GL_TEXTURE0 + unit);
        bind();
    }

    public void unbind() {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    public void cleanup() {
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
        }
    }

    public int getTextureId() {
        return textureId;
    }
}

