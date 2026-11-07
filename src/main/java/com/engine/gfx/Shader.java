package com.engine.gfx;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Shader {
    private final int programId;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public Shader(String vertexSource, String fragmentSource) {
        int vShader = compileShader(vertexSource, GL20.GL_VERTEX_SHADER);
        int fShader = compileShader(fragmentSource, GL20.GL_FRAGMENT_SHADER);

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vShader);
        GL20.glAttachShader(programId, fShader);
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(programId);
            throw new RuntimeException("Failed to link shader program: " + log);
        }

        GL20.glDetachShader(programId, vShader);
        GL20.glDetachShader(programId, fShader);
        GL20.glDeleteShader(vShader);
        GL20.glDeleteShader(fShader);
    }

    public static Shader fromResources(String vertPath, String fragPath) {
        String vert = loadResource(vertPath);
        String frag = loadResource(fragPath);
        return new Shader(vert, frag);
    }

    private static String loadResource(String path) {
        try (InputStream in = Shader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Resource not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read shader resource: " + path, e);
        }
    }

    private int compileShader(String source, int type) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);

        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
            String typeStr = (type == GL20.GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            String log = GL20.glGetShaderInfoLog(id);
            throw new RuntimeException(typeStr + " Shader Compilation Error: " + log);
        }
        return id;
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    private int getUniformLocation(String name) {
        return uniforms.computeIfAbsent(name, n -> GL20.glGetUniformLocation(programId, n));
    }

    public void setUniform(String name, int value) {
        GL20.glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform(String name, float value) {
        GL20.glUniform1f(getUniformLocation(name), value);
    }

    public void setUniform(String name, Vector3f vector) {
        GL20.glUniform3f(getUniformLocation(name), vector.x, vector.y, vector.z);
    }

    public void setUniform(String name, float x, float y, float z) {
        GL20.glUniform3f(getUniformLocation(name), x, y, z);
    }

    public void setUniform(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            GL20.glUniformMatrix4fv(getUniformLocation(name), false, fb);
        }
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }
    }
}

