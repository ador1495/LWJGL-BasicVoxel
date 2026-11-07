package com.engine.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

public class Window {
    private final String title;
    private int width;
    private int height;
    private long handle;
    private boolean resized;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.resized = false;
    }

    public void init() {
        init(true);
    }

    public void init(boolean visible) {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        handle = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        GLFW.glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            this.width = Math.max(1, w);
            this.height = Math.max(1, h);
            this.resized = true;
            GL11.glViewport(0, 0, this.width, this.height);
        });

        // Center window on primary monitor
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (vidmode != null) {
            GLFW.glfwSetWindowPos(handle, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        }

        GLFW.glfwMakeContextCurrent(handle);
        GL.createCapabilities();

        // Enable V-Sync by default (disabled in headless/test mode)
        GLFW.glfwSwapInterval(visible ? 1 : 0);

        if (visible) {
            GLFW.glfwShowWindow(handle);
        }

        // Initial viewport
        GL11.glViewport(0, 0, width, height);
    }

    public void update() {
        GLFW.glfwSwapBuffers(handle);
        GLFW.glfwPollEvents();
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    public void setTitle(String newTitle) {
        GLFW.glfwSetWindowTitle(handle, newTitle);
    }

    public void destroy() {
        GLFW.glfwDestroyWindow(handle);
        GLFW.glfwTerminate();
        GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    public long getHandle() {
        return handle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isResized() {
        return resized;
    }

    public void setResized(boolean resized) {
        this.resized = resized;
    }
}
