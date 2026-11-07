package com.engine.core;

import org.lwjgl.glfw.GLFW;

public class Time {
    private static double lastTime;
    private static float deltaTime;
    private static int frames;
    private static int fps;
    private static double fpsTimer;

    public static void init() {
        lastTime = GLFW.glfwGetTime();
        fpsTimer = lastTime;
        deltaTime = 0f;
        frames = 0;
        fps = 0;
    }

    public static void update() {
        double currentTime = GLFW.glfwGetTime();
        deltaTime = (float) (currentTime - lastTime);
        lastTime = currentTime;

        // Clamp delta time to avoid physics explosion during hitch/freeze
        if (deltaTime > 0.1f) {
            deltaTime = 0.1f;
        }

        frames++;
        if (currentTime - fpsTimer >= 1.0) {
            fps = frames;
            frames = 0;
            fpsTimer += 1.0;
        }
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static int getFps() {
        return fps;
    }

    public static double getTime() {
        return GLFW.glfwGetTime();
    }
}

