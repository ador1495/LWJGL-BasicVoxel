package com.engine.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position = new Vector3f();
    private final Vector3f front = new Vector3f(0, 0, -1);
    private final Vector3f up = new Vector3f(0, 1, 0);
    private final Vector3f right = new Vector3f(1, 0, 0);
    private final Vector3f worldUp = new Vector3f(0, 1, 0);

    private float yaw = -90.0f;
    private float pitch = 0.0f;
    private float fov = 75.0f;
    private float nearPlane = 0.05f;
    private float farPlane = 500.0f;

    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewProjectionMatrix = new Matrix4f();
    private final Frustum frustum = new Frustum();

    public Camera(float x, float y, float z) {
        this.position.set(x, y, z);
        updateVectors();
    }

    public void update(int windowWidth, int windowHeight) {
        updateVectors();

        float aspect = (float) windowWidth / Math.max(1, windowHeight);
        projectionMatrix.identity().perspective((float) Math.toRadians(fov), aspect, nearPlane, farPlane);

        Vector3f target = new Vector3f(position).add(front);
        viewMatrix.identity().lookAt(position, target, up);

        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
        frustum.update(viewProjectionMatrix);
    }

    public void processMouseMovement(float xOffset, float yOffset) {
        float sensitivity = 0.12f;
        xOffset *= sensitivity;
        yOffset *= sensitivity;

        yaw += xOffset;
        pitch -= yOffset;

        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        updateVectors();
    }

    private void updateVectors() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        front.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        front.y = (float) Math.sin(pitchRad);
        front.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        front.normalize();

        front.cross(worldUp, right).normalize();
        right.cross(front, up).normalize();
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getFront() {
        return front;
    }

    public Vector3f getRight() {
        return right;
    }

    public Vector3f getUp() {
        return up;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getViewProjectionMatrix() {
        return viewProjectionMatrix;
    }

    public Frustum getFrustum() {
        return frustum;
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }
}

