package com.engine.player;

import com.engine.util.AABB;
import com.engine.world.Block;
import com.engine.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class PlayerPhysics {
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float EYE_OFFSET = 1.62f;

    public static final float GRAVITY = -28.0f;
    public static final float JUMP_SPEED = 8.5f;

    private final AABB playerBox = new AABB();
    private final List<AABB> candidateBoxes = new ArrayList<>(64);

    public void update(Vector3f pos, Vector3f vel, boolean isFlying, boolean onGroundTarget[], World world, float dt) {
        if (isFlying) {
            // Creative flight: pure kinematic movement without voxel collision stops
            pos.x += vel.x * dt;
            pos.y += vel.y * dt;
            pos.z += vel.z * dt;
            // High damping in flight
            vel.x *= Math.pow(0.1, dt);
            vel.y *= Math.pow(0.1, dt);
            vel.z *= Math.pow(0.1, dt);
            onGroundTarget[0] = false;
            return;
        }

        // Apply gravity
        vel.y += GRAVITY * dt;
        if (vel.y < -50.0f) vel.y = -50.0f; // Terminal velocity

        // Update player AABB
        updateBox(pos);

        // Fetch surrounding solid block AABBs
        float moveX = vel.x * dt;
        float moveY = vel.y * dt;
        float moveZ = vel.z * dt;

        AABB broadphase = new AABB(
            Math.min(playerBox.minX, playerBox.minX + moveX) - 1,
            Math.min(playerBox.minY, playerBox.minY + moveY) - 1,
            Math.min(playerBox.minZ, playerBox.minZ + moveZ) - 1,
            Math.max(playerBox.maxX, playerBox.maxX + moveX) + 1,
            Math.max(playerBox.maxY, playerBox.maxY + moveY) + 1,
            Math.max(playerBox.maxZ, playerBox.maxZ + moveZ) + 1
        );

        getCandidateBoxes(broadphase, world);

        // Axis-by-axis collision resolution (Y first, then X, then Z)
        boolean onGround = false;

        // 1. Move Y
        if (moveY != 0) {
            for (AABB box : candidateBoxes) {
                if (overlapsXZ(playerBox, box)) {
                    if (moveY > 0 && playerBox.maxY <= box.minY) {
                        float maxMove = box.minY - playerBox.maxY;
                        if (maxMove < moveY) {
                            moveY = maxMove;
                            vel.y = 0;
                        }
                    } else if (moveY < 0 && playerBox.minY >= box.maxY) {
                        float maxMove = box.maxY - playerBox.minY;
                        if (maxMove > moveY) {
                            moveY = maxMove;
                            vel.y = 0;
                            onGround = true;
                        }
                    }
                }
            }
        }
        playerBox.offset(0, moveY, 0);
        pos.y += moveY;

        // 2. Move X
        if (moveX != 0) {
            for (AABB box : candidateBoxes) {
                if (overlapsYZ(playerBox, box)) {
                    if (moveX > 0 && playerBox.maxX <= box.minX) {
                        float maxMove = box.minX - playerBox.maxX;
                        if (maxMove < moveX) {
                            moveX = maxMove;
                            vel.x = 0;
                        }
                    } else if (moveX < 0 && playerBox.minX >= box.maxX) {
                        float maxMove = box.maxX - playerBox.minX;
                        if (maxMove > moveX) {
                            moveX = maxMove;
                            vel.x = 0;
                        }
                    }
                }
            }
        }
        playerBox.offset(moveX, 0, 0);
        pos.x += moveX;

        // 3. Move Z
        if (moveZ != 0) {
            for (AABB box : candidateBoxes) {
                if (overlapsXY(playerBox, box)) {
                    if (moveZ > 0 && playerBox.maxZ <= box.minZ) {
                        float maxMove = box.minZ - playerBox.maxZ;
                        if (maxMove < moveZ) {
                            moveZ = maxMove;
                            vel.z = 0;
                        }
                    } else if (moveZ < 0 && playerBox.minZ >= box.maxZ) {
                        float maxMove = box.maxZ - playerBox.minZ;
                        if (maxMove > moveZ) {
                            moveZ = maxMove;
                            vel.z = 0;
                        }
                    }
                }
            }
        }
        playerBox.offset(0, 0, moveZ);
        pos.z += moveZ;

        onGroundTarget[0] = onGround;

        // Ground/Air friction
        float friction = onGround ? 0.65f : 0.92f;
        vel.x *= Math.pow(friction, dt * 20.0f);
        vel.z *= Math.pow(friction, dt * 20.0f);
    }

    private void updateBox(Vector3f pos) {
        float halfW = PLAYER_WIDTH / 2.0f;
        playerBox.set(
            pos.x - halfW, pos.y, pos.z - halfW,
            pos.x + halfW, pos.y + PLAYER_HEIGHT, pos.z + halfW
        );
    }

    private void getCandidateBoxes(AABB area, World world) {
        candidateBoxes.clear();
        int minX = (int) Math.floor(area.minX);
        int maxX = (int) Math.ceil(area.maxX);
        int minY = (int) Math.floor(area.minY);
        int maxY = (int) Math.ceil(area.maxY);
        int minZ = (int) Math.floor(area.minZ);
        int maxZ = (int) Math.ceil(area.maxZ);

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    byte b = world.getBlock(x, y, z);
                    if (b != 0 && Block.getById(b).solid) {
                        candidateBoxes.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
    }

    private static boolean overlapsXZ(AABB a, AABB b) {
        return a.maxX > b.minX && a.minX < b.maxX && a.maxZ > b.minZ && a.minZ < b.maxZ;
    }

    private static boolean overlapsYZ(AABB a, AABB b) {
        return a.maxY > b.minY && a.minY < b.maxY && a.maxZ > b.minZ && a.minZ < b.maxZ;
    }

    private static boolean overlapsXY(AABB a, AABB b) {
        return a.maxX > b.minX && a.minX < b.maxX && a.maxY > b.minY && a.minY < b.maxY;
    }
}

