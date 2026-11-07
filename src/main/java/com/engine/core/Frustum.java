package com.engine.core;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Frustum {
    // 6 planes: Left, Right, Bottom, Top, Near, Far
    // Each plane has equation Ax + By + Cz + D = 0
    private final Vector4f[] planes = new Vector4f[6];

    public Frustum() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new Vector4f();
        }
    }

    public void update(Matrix4f m) {
        // Left plane
        planes[0].set(m.m03() + m.m00(), m.m13() + m.m10(), m.m23() + m.m20(), m.m33() + m.m30());
        // Right plane
        planes[1].set(m.m03() - m.m00(), m.m13() - m.m10(), m.m23() - m.m20(), m.m33() - m.m30());
        // Bottom plane
        planes[2].set(m.m03() + m.m01(), m.m13() + m.m11(), m.m23() + m.m21(), m.m33() + m.m31());
        // Top plane
        planes[3].set(m.m03() - m.m01(), m.m13() - m.m11(), m.m23() - m.m21(), m.m33() - m.m31());
        // Near plane
        planes[4].set(m.m03() + m.m02(), m.m13() + m.m12(), m.m23() + m.m22(), m.m33() + m.m32());
        // Far plane
        planes[5].set(m.m03() - m.m02(), m.m13() - m.m12(), m.m23() - m.m22(), m.m33() - m.m32());

        // Normalize the planes
        for (int i = 0; i < 6; i++) {
            Vector4f plane = planes[i];
            float length = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
            if (length > 0f) {
                plane.div(length);
            }
        }
    }

    /**
     * Tests if an Axis-Aligned Bounding Box (AABB) is inside or intersects the frustum.
     * Returns false if the box is completely outside any of the 6 planes.
     */
    public boolean testAABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            Vector4f p = planes[i];
            // Test the p-vertex (the corner most in the direction of the normal)
            float px = (p.x > 0f) ? maxX : minX;
            float py = (p.y > 0f) ? maxY : minY;
            float pz = (p.z > 0f) ? maxZ : minZ;

            if (p.x * px + p.y * py + p.z * pz + p.w < 0f) {
                return false; // Box is completely behind this plane
            }
        }
        return true;
    }
}

