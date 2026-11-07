package com.engine.gfx;

import com.engine.core.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

public class BlockOutlineRenderer {
    private final int vao;
    private final int vbo;
    private final Shader shader;

    private static final String VERTEX_SHADER = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        uniform mat4 u_ViewProj;
        uniform vec3 u_BlockPos;
        void main() {
            // Slight expansion to prevent z-fighting with block faces
            vec3 p = u_BlockPos + aPos * 1.002 - vec3(0.001);
            gl_Position = u_ViewProj * vec4(p, 1.0);
        }
    """;

    private static final String FRAGMENT_SHADER = """
        #version 330 core
        out vec4 FragColor;
        void main() {
            FragColor = vec4(0.0, 0.0, 0.0, 0.7); // Dark semi-transparent outline
        }
    """;

    public BlockOutlineRenderer() {
        shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);

        // 12 edges of a cube (24 vertices)
        float[] lineVertices = {
            // Bottom 4 edges
            0, 0, 0,  1, 0, 0,
            1, 0, 0,  1, 0, 1,
            1, 0, 1,  0, 0, 1,
            0, 0, 1,  0, 0, 0,
            // Top 4 edges
            0, 1, 0,  1, 1, 0,
            1, 1, 0,  1, 1, 1,
            1, 1, 1,  0, 1, 1,
            0, 1, 1,  0, 1, 0,
            // Vertical 4 edges
            0, 0, 0,  0, 1, 0,
            1, 0, 0,  1, 1, 0,
            1, 0, 1,  1, 1, 1,
            0, 0, 1,  0, 1, 1
        };

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer fb = BufferUtils.createFloatBuffer(lineVertices.length);
        fb.put(lineVertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL30.glBindVertexArray(0);
    }

    public void render(Camera camera, Vector3i blockPos) {
        if (blockPos == null) return;

        GL11.glLineWidth(2.5f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        shader.setUniform("u_ViewProj", camera.getViewProjectionMatrix());
        shader.setUniform("u_BlockPos", (float) blockPos.x, (float) blockPos.y, (float) blockPos.z);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
        GL30.glBindVertexArray(0);

        shader.unbind();
        GL11.glLineWidth(1.0f);
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vao);
        shader.cleanup();
    }
}

