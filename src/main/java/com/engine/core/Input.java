package com.engine.core;

import org.lwjgl.glfw.GLFW;

public class Input {
    private static final boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private static final boolean[] keysPressed = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private static final boolean[] mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
    private static final boolean[] mouseButtonsPressed = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

    private static double mouseX;
    private static double mouseY;
    private static double lastMouseX;
    private static double lastMouseY;
    private static float mouseDeltaX;
    private static float mouseDeltaY;
    private static boolean firstMouse = true;
    private static boolean mouseLocked = true;
    private static double scrollY = 0;

    public static void init(long window) {
        GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) {
                if (action == GLFW.GLFW_PRESS) {
                    keys[key] = true;
                    keysPressed[key] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    keys[key] = false;
                }
            }
        });

        GLFW.glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button >= 0 && button < mouseButtons.length) {
                if (action == GLFW.GLFW_PRESS) {
                    mouseButtons[button] = true;
                    mouseButtonsPressed[button] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    mouseButtons[button] = false;
                }
            }
        });

        GLFW.glfwSetCursorPosCallback(window, (w, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            mouseX = xpos;
            mouseY = ypos;
        });

        GLFW.glfwSetScrollCallback(window, (w, xoffset, yoffset) -> {
            scrollY = yoffset;
        });

        setMouseLocked(window, true);
    }

    public static void update() {
        if (mouseLocked) {
            mouseDeltaX = (float) (mouseX - lastMouseX);
            mouseDeltaY = (float) (mouseY - lastMouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        } else {
            mouseDeltaX = 0;
            mouseDeltaY = 0;
        }

        // Reset single-frame pressed flags
        for (int i = 0; i < keysPressed.length; i++) {
            keysPressed[i] = false;
        }
        for (int i = 0; i < mouseButtonsPressed.length; i++) {
            mouseButtonsPressed[i] = false;
        }
        scrollY = 0;
    }

    public static boolean isKeyDown(int keyCode) {
        if (keyCode >= 0 && keyCode < keys.length) {
            return keys[keyCode];
        }
        return false;
    }

    public static boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keys.length) {
            return keysPressed[keyCode];
        }
        return false;
    }

    public static boolean isMouseButtonDown(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }

    public static boolean isMouseButtonPressed(int button) {
        if (button >= 0 && button < mouseButtonsPressed.length) {
            return mouseButtonsPressed[button];
        }
        return false;
    }

    public static float getMouseDeltaX() {
        return mouseDeltaX;
    }

    public static float getMouseDeltaY() {
        return mouseDeltaY;
    }

    public static double getScrollY() {
        return scrollY;
    }

    public static boolean isMouseLocked() {
        return mouseLocked;
    }

    public static void setMouseLocked(long window, boolean locked) {
        mouseLocked = locked;
        if (locked) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            firstMouse = true;
        } else {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }
}

