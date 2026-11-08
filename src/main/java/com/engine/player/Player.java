package com.engine.player;

import com.engine.core.Camera;
import com.engine.core.Input;
import com.engine.world.Block;
import com.engine.world.World;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Player {
    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private final Camera camera;
    private final PlayerPhysics physics = new PlayerPhysics();
    private final boolean[] onGroundWrapper = new boolean[1];

    private boolean flying = false;
    private double lastSpacePressTime = 0.0;
    private Block selectedBlock = Block.STONE;
    private Raycast.RaycastResult targetBlock = new Raycast.RaycastResult();

    // Hotbar blocks corresponding to keys 1-9: Slot 3 (index 2) is Torch
    private static final Block[] HOTBAR = {
        Block.STONE, Block.DIRT, Block.TORCH, Block.COBBLESTONE,
        Block.WOOD, Block.PLANKS, Block.LEAVES, Block.SAND, Block.GLASS
    };
    private int hotbarIndex = 0;

    public Player(float startX, float startY, float startZ) {
        this.position.set(startX, startY, startZ);
        this.camera = new Camera(startX, startY + PlayerPhysics.EYE_OFFSET, startZ);
    }

    public void update(World world, float dt) {
        // Double-click Space to toggle flying mode
        if (Input.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            double now = com.engine.core.Time.getTime();
            if (now - lastSpacePressTime <= 0.35) {
                flying = !flying;
                velocity.set(0, 0, 0);
                lastSpacePressTime = 0.0;
            } else {
                lastSpacePressTime = now;
            }
        }

        // Toggle flying mode with 'F' key as well
        if (Input.isKeyPressed(GLFW.GLFW_KEY_F)) {
            flying = !flying;
            velocity.set(0, 0, 0);
        }

        // Hotbar selection with number keys 1-9
        for (int i = 0; i < 9; i++) {
            if (Input.isKeyPressed(GLFW.GLFW_KEY_1 + i)) {
                hotbarIndex = i;
                selectedBlock = HOTBAR[i];
            }
        }

        // Scroll wheel to cycle hotbar
        double scroll = Input.getScrollY();
        if (scroll != 0) {
            hotbarIndex = (hotbarIndex - (int) Math.signum(scroll) + HOTBAR.length) % HOTBAR.length;
            selectedBlock = HOTBAR[hotbarIndex];
        }

        // Mouse look
        camera.processMouseMovement(Input.getMouseDeltaX(), Input.getMouseDeltaY());

        // Movement input
        handleMovement(dt);

        // Physics step
        physics.update(position, velocity, flying, onGroundWrapper, world, dt);

        // Sync camera position
        camera.getPosition().set(position.x, position.y + PlayerPhysics.EYE_OFFSET, position.z);

        // Voxel Raycasting for block targeting
        targetBlock = Raycast.cast(camera.getPosition(), camera.getFront(), 6.0f, world);

        // Block Break (Left Click)
        if (Input.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
            if (targetBlock.hit && targetBlock.blockId != Block.BEDROCK.id) {
                world.setBlock(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z, (byte) 0);
            }
        }

        // Block Place (Right Click)
        if (Input.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (targetBlock.hit && selectedBlock != null && selectedBlock != Block.AIR) {
                // Ensure placed block doesn't intersect player's body
                int px = targetBlock.adjacentPos.x;
                int py = targetBlock.adjacentPos.y;
                int pz = targetBlock.adjacentPos.z;

                boolean collidesWithPlayer = !flying && selectedBlock.solid &&
                    (position.x - 0.35f < px + 1 && position.x + 0.35f > px &&
                     position.y < py + 1 && position.y + PlayerPhysics.PLAYER_HEIGHT > py &&
                     position.z - 0.35f < pz + 1 && position.z + 0.35f > pz);

                if (!collidesWithPlayer) {
                    world.setBlock(px, py, pz, selectedBlock.id);
                }
            }
        }
    }

    private void handleMovement(float dt) {
        boolean forward = Input.isKeyDown(GLFW.GLFW_KEY_W);
        boolean backward = Input.isKeyDown(GLFW.GLFW_KEY_S);
        boolean left = Input.isKeyDown(GLFW.GLFW_KEY_A);
        boolean right = Input.isKeyDown(GLFW.GLFW_KEY_D);
        boolean jump = Input.isKeyDown(GLFW.GLFW_KEY_SPACE);
        boolean sneak = Input.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
        boolean sprint = Input.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL);

        Vector3f wishDir = new Vector3f();
        Vector3f front = new Vector3f(camera.getFront());
        Vector3f camRight = new Vector3f(camera.getRight());

        if (!flying) {
            front.y = 0;
            front.normalize();
        }
        camRight.y = 0;
        camRight.normalize();

        if (forward) wishDir.add(front);
        if (backward) wishDir.sub(front);
        if (right) wishDir.add(camRight);
        if (left) wishDir.sub(camRight);

        if (wishDir.lengthSquared() > 0.0001f) {
            wishDir.normalize();
        }

        float speed = sprint ? 8.5f : 4.5f;
        if (flying) {
            speed *= 2.5f;
            wishDir.mul(speed);
            velocity.x = wishDir.x;
            velocity.z = wishDir.z;

            if (jump) velocity.y = speed;
            else if (sneak) velocity.y = -speed;
            else velocity.y = 0;
        } else {
            // Normal walking / jumping
            wishDir.mul(speed);
            // Accelerate horizontally
            float accel = onGroundWrapper[0] ? 15.0f : 4.0f;
            velocity.x += (wishDir.x - velocity.x) * Math.min(1.0f, accel * dt);
            velocity.z += (wishDir.z - velocity.z) * Math.min(1.0f, accel * dt);

            if (jump && onGroundWrapper[0]) {
                velocity.y = PlayerPhysics.JUMP_SPEED;
            }
        }
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isOnGround() {
        return onGroundWrapper[0];
    }

    public Block getSelectedBlock() {
        return selectedBlock;
    }

    public int getHotbarIndex() {
        return hotbarIndex;
    }

    public Raycast.RaycastResult getTargetBlock() {
        return targetBlock;
    }

    public boolean isHoldingTorch() {
        return selectedBlock == Block.TORCH;
    }
}

