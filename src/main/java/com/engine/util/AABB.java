package com.engine.util;

import org.joml.Vector3f;

public class AABB {
    public float minX, minY, minZ;
    public float maxX, maxY, maxZ;

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB() {
        this(0, 0, 0, 0, 0, 0);
    }

    public void set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public void set(AABB other) {
        set(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    public void offset(float x, float y, float z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
    }

    public boolean intersects(AABB other) {
        return this.maxX > other.minX && this.minX < other.maxX &&
               this.maxY > other.minY && this.minY < other.maxY &&
               this.maxZ > other.minZ && this.minZ < other.maxZ;
    }

    public boolean contains(float x, float y, float z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    public float getWidth() {
        return maxX - minX;
    }

    public float getHeight() {
        return maxY - minY;
    }

    public float getDepth() {
        return maxZ - minZ;
    }
}

