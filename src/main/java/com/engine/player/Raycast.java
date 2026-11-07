package com.engine.player;

import com.engine.world.Block;
import com.engine.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Raycast {
    public static class RaycastResult {
        public boolean hit = false;
        public final Vector3i blockPos = new Vector3i();
        public final Vector3i adjacentPos = new Vector3i();
        public final Vector3i normal = new Vector3i();
        public byte blockId = 0;
    }

    /**
     * Amanatides & Woo Fast Voxel Traversal Algorithm (DDA)
     */
    public static RaycastResult cast(Vector3f origin, Vector3f dir, float maxDist, World world) {
        RaycastResult result = new RaycastResult();

        if (dir.lengthSquared() < 0.0001f) {
            return result;
        }

        float dx = dir.x;
        float dy = dir.y;
        float dz = dir.z;

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = (dx > 0) ? 1 : (dx < 0 ? -1 : 0);
        int stepY = (dy > 0) ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = (dz > 0) ? 1 : (dz < 0 ? -1 : 0);

        float tDeltaX = (stepX != 0) ? Math.abs(1.0f / dx) : Float.POSITIVE_INFINITY;
        float tDeltaY = (stepY != 0) ? Math.abs(1.0f / dy) : Float.POSITIVE_INFINITY;
        float tDeltaZ = (stepZ != 0) ? Math.abs(1.0f / dz) : Float.POSITIVE_INFINITY;

        float tMaxX = (stepX != 0) ? ((stepX > 0) ? (x + 1.0f - origin.x) / dx : (origin.x - x) / -dx) : Float.POSITIVE_INFINITY;
        float tMaxY = (stepY != 0) ? ((stepY > 0) ? (y + 1.0f - origin.y) / dy : (origin.y - y) / -dy) : Float.POSITIVE_INFINITY;
        float tMaxZ = (stepZ != 0) ? ((stepZ > 0) ? (z + 1.0f - origin.z) / dz : (origin.z - z) / -dz) : Float.POSITIVE_INFINITY;

        int normalX = 0;
        int normalY = 0;
        int normalZ = 0;

        float distance = 0f;
        int maxSteps = (int) (maxDist * 3.0f) + 16;
        int stepCount = 0;

        while (distance <= maxDist && stepCount++ < maxSteps) {
            byte b = world.getBlock(x, y, z);
            if (b != 0 && Block.getById(b).solid) {
                result.hit = true;
                result.blockPos.set(x, y, z);
                result.normal.set(normalX, normalY, normalZ);
                result.adjacentPos.set(x + normalX, y + normalY, z + normalZ);
                result.blockId = b;
                return result;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    distance = tMaxX;
                    tMaxX += tDeltaX;
                    normalX = -stepX;
                    normalY = 0;
                    normalZ = 0;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    normalX = 0;
                    normalY = 0;
                    normalZ = -stepZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    distance = tMaxY;
                    tMaxY += tDeltaY;
                    normalX = 0;
                    normalY = -stepY;
                    normalZ = 0;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    normalX = 0;
                    normalY = 0;
                    normalZ = -stepZ;
                }
            }
        }

        return result;
    }
}
