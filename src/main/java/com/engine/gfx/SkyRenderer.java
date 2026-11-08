package com.engine.gfx;

import com.engine.core.Camera;
import com.engine.core.Time;
import com.engine.world.DaylightCycle;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

public class SkyRenderer {
    private final Shader skyShader;
    private int vao;
    private int vbo;

    private final Matrix4f invViewProj = new Matrix4f();
    private final Matrix4f viewNoTrans = new Matrix4f();
    private final Vector3f zenithColor = new Vector3f();

    public SkyRenderer() {
        this.skyShader = Shader.fromResources("/shaders/sky.vert", "/shaders/sky.frag");
        setupMesh();
    }

    private void setupMesh() {
        // Fullscreen quad in NDC [-1, 1]
        float[] quadVertices = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f, -1.0f,
             1.0f,  1.0f
        };

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(quadVertices.length);
        buffer.put(quadVertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void render(Camera camera, DaylightCycle daylightCycle) {
        Vector3f sunDir = daylightCycle.getSunDir();
        float elev = sunDir.y;
        if (elev > 0.20f) {
            zenithColor.set(0.24f, 0.48f, 0.88f);
        } else if (elev > -0.05f) {
            float t = (elev + 0.05f) / 0.25f;
            zenithColor.set(
                0.12f + (0.24f - 0.12f) * t,
                0.15f + (0.48f - 0.15f) * t,
                0.35f + (0.88f - 0.35f) * t
            );
        } else {
            zenithColor.set(0.02f, 0.03f, 0.07f);
        }

        // View matrix without translation (camera orientation only)
        viewNoTrans.set(camera.getViewMatrix());
        viewNoTrans.m30(0);
        viewNoTrans.m31(0);
        viewNoTrans.m32(0);

        // Inverse of ViewProjection without camera translation
        invViewProj.set(camera.getProjectionMatrix()).mul(viewNoTrans).invert();

        skyShader.bind();
        skyShader.setUniform("u_InvViewProj", invViewProj);
        skyShader.setUniform("u_SunDir", sunDir);
        skyShader.setUniform("u_LightColor", daylightCycle.getLightColor());
        skyShader.setUniform("u_SkyColor", daylightCycle.getSkyColor());
        skyShader.setUniform("u_ZenithColor", zenithColor);
        skyShader.setUniform("u_Time", (float) Time.getTime());

        // Sky rendering state: write disabled, depth test passes at far plane (LEQUAL)
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        // Restore OpenGL state
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        skyShader.unbind();
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        skyShader.cleanup();
    }
}

