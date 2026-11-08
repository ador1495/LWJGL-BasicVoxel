package com.engine.gfx;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

public class HudRenderer {
    private final Shader shader;
    private final int crosshairVao;
    private final int crosshairVbo;
    private final int quadVao;
    private final int quadVbo;

    private static final String VERTEX_SHADER = """
        #version 330 core
        layout (location = 0) in vec2 aPos;
        layout (location = 1) in vec4 aColor;
        uniform mat4 u_Ortho;
        out vec4 v_Color;
        void main() {
            gl_Position = u_Ortho * vec4(aPos, 0.0, 1.0);
            v_Color = aColor;
        }
    """;

    private static final String FRAGMENT_SHADER = """
        #version 330 core
        in vec4 v_Color;
        out vec4 FragColor;
        void main() {
            FragColor = v_Color;
        }
    """;

    public HudRenderer() {
        shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);

        // Crosshair (+) mesh: 10 pixels long each arm
        float s = 10.0f;
        float[] chVertices = {
            -s, 0.0f,  1, 1, 1, 0.9f,
             s, 0.0f,  1, 1, 1, 0.9f,
            0.0f, -s,  1, 1, 1, 0.9f,
            0.0f,  s,  1, 1, 1, 0.9f
        };

        crosshairVao = GL30.glGenVertexArrays();
        crosshairVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(crosshairVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, crosshairVbo);

        FloatBuffer chBuffer = BufferUtils.createFloatBuffer(chVertices.length);
        chBuffer.put(chVertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, chBuffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 6 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        // Quad for hotbar slots
        quadVao = GL30.glGenVertexArrays();
        quadVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(quadVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 1024 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 6 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL30.glBindVertexArray(0);
    }

    public void render(int width, int height, int selectedSlot) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -1, 1);

        shader.bind();
        shader.setUniform("u_Ortho", ortho);

        // 1. Draw Hotbar at bottom center
        renderHotbar(width, height, selectedSlot);

        // 2. Draw Crosshair at center
        Matrix4f chOrtho = new Matrix4f().ortho(-width / 2.0f, width / 2.0f, -height / 2.0f, height / 2.0f, -1, 1);
        shader.setUniform("u_Ortho", chOrtho);

        GL11.glLineWidth(2.0f);
        GL30.glBindVertexArray(crosshairVao);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 4);
        GL30.glBindVertexArray(0);

        shader.unbind();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void renderHotbar(int screenW, int screenH, int selectedSlot) {
        int slots = 9;
        float slotSize = 40.0f;
        float padding = 4.0f;
        float totalWidth = slots * slotSize + (slots - 1) * padding;
        float startX = (screenW - totalWidth) / 2.0f;
        float startY = screenH - slotSize - 16.0f;

        // Distinct preview colors for the 9 hotbar blocks:
        // Stone, Dirt, Torch, Cobblestone, Wood, Planks, Leaves, Sand, Glass
        float[][] blockColors = {
            {0.5f, 0.5f, 0.5f},      // Stone
            {0.45f, 0.32f, 0.2f},    // Dirt
            {1.0f, 0.72f, 0.22f},    // Torch (Gold/Flame)
            {0.38f, 0.38f, 0.38f},   // Cobblestone
            {0.4f, 0.3f, 0.18f},     // Wood
            {0.7f, 0.55f, 0.35f},    // Planks
            {0.2f, 0.5f, 0.15f},     // Leaves
            {0.85f, 0.8f, 0.6f},     // Sand
            {0.8f, 0.95f, 1.0f}      // Glass
        };

        FloatBuffer buffer = BufferUtils.createFloatBuffer(slots * 12 * 6);

        for (int i = 0; i < slots; i++) {
            float x0 = startX + i * (slotSize + padding);
            float y0 = startY;
            float x1 = x0 + slotSize;
            float y1 = y0 + slotSize;

            boolean isSelected = (i == selectedSlot);

            // Slot Background quad
            float bgAlpha = isSelected ? 0.75f : 0.45f;
            float r = isSelected ? 1.0f : 0.15f;
            float g = isSelected ? 1.0f : 0.15f;
            float b = isSelected ? 0.3f : 0.15f;

            // Border / Slot Background
            putQuad(buffer, x0, y0, x1, y1, r, g, b, bgAlpha);

            // Inner Block Icon quad
            float inset = 6.0f;
            float[] col = blockColors[i];
            putQuad(buffer, x0 + inset, y0 + inset, x1 - inset, y1 - inset, col[0], col[1], col[2], 0.95f);
        }
        buffer.flip();

        GL30.glBindVertexArray(quadVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);

        // Each quad is 6 vertices
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, slots * 12);
        GL30.glBindVertexArray(0);
    }

    private void putQuad(FloatBuffer b, float x0, float y0, float x1, float y1, float r, float g, float bl, float a) {
        // Triangle 1
        b.put(x0).put(y0).put(r).put(g).put(bl).put(a);
        b.put(x1).put(y0).put(r).put(g).put(bl).put(a);
        b.put(x1).put(y1).put(r).put(g).put(bl).put(a);

        // Triangle 2
        b.put(x0).put(y0).put(r).put(g).put(bl).put(a);
        b.put(x1).put(y1).put(r).put(g).put(bl).put(a);
        b.put(x0).put(y1).put(r).put(g).put(bl).put(a);
    }

    public void cleanup() {
        GL15.glDeleteBuffers(crosshairVbo);
        GL30.glDeleteVertexArrays(crosshairVao);
        GL15.glDeleteBuffers(quadVbo);
        GL30.glDeleteVertexArrays(quadVao);
        shader.cleanup();
    }
}

