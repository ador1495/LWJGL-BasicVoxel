package com.engine.gfx;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

public class Mesh {
    private int vao;
    private int vbo;
    private int ebo;
    private int indexCount;
    private boolean allocated;

    public Mesh() {
        this.vao = 0;
        this.vbo = 0;
        this.ebo = 0;
        this.indexCount = 0;
        this.allocated = false;
    }

    public void upload(IntBuffer vertexData, IntBuffer indexData, int count) {
        if (!allocated) {
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            ebo = GL15.glGenBuffers();
            allocated = true;
        }

        this.indexCount = count;
        if (count == 0) return;

        GL30.glBindVertexArray(vao);

        // Upload vertex buffer (packed 32-bit integers, 1 int = 4 bytes per vertex)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexData, GL15.GL_DYNAMIC_DRAW);

        // Upload element/index buffer
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexData, GL15.GL_DYNAMIC_DRAW);

        // Attribute 0: packed 32-bit integer
        // Use glVertexAttribIPointer so the bit pattern is kept as pure integer in shader!
        GL30.glVertexAttribIPointer(0, 1, GL11.GL_INT, Integer.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL30.glBindVertexArray(0);
    }

    public void render() {
        if (!allocated || indexCount == 0) return;

        GL30.glBindVertexArray(vao);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public boolean isEmpty() {
        return indexCount == 0;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public void cleanup() {
        if (allocated) {
            GL15.glDeleteBuffers(vbo);
            GL15.glDeleteBuffers(ebo);
            GL30.glDeleteVertexArrays(vao);
            allocated = false;
            indexCount = 0;
        }
    }
}

