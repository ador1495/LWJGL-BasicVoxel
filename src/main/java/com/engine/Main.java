package com.engine;

import com.engine.core.Input;
import com.engine.core.Time;
import com.engine.core.Window;
import com.engine.gfx.BlockOutlineRenderer;
import com.engine.gfx.HudRenderer;
import com.engine.gfx.Shader;
import com.engine.gfx.TextureArray;
import com.engine.player.Player;
import com.engine.world.World;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class Main {
    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 720;
    private static final String APP_TITLE = "VoxelCraft 3D | Performance Voxel Engine";

    public static void main(String[] args) {
        boolean testMode = args.length > 0 && args[0].equalsIgnoreCase("--test");

        System.out.println("[VoxelEngine] Initializing Window & OpenGL...");
        Window window = new Window(APP_TITLE, INITIAL_WIDTH, INITIAL_HEIGHT);
        window.init(!testMode);
        System.out.println("[VoxelEngine] Window & Context initialized.");

        if (testMode) {
            GLFW.glfwSwapInterval(0); // Uncap framerate in test mode
        }

        // Configure modern OpenGL state
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);

        // Sky atmosphere clear color
        GL11.glClearColor(0.65f, 0.82f, 1.0f, 1.0f);

        System.out.println("[VoxelEngine] Initializing Input & Time...");
        Input.init(window.getHandle());
        Time.init();

        System.out.println("[VoxelEngine] Loading Shaders & Textures...");
        Shader chunkShader = Shader.fromResources("/shaders/chunk.vert", "/shaders/chunk.frag");
        TextureArray textures = new TextureArray();
        BlockOutlineRenderer outlineRenderer = new BlockOutlineRenderer();
        HudRenderer hudRenderer = new HudRenderer();

        System.out.println("[VoxelEngine] Initializing World & Player...");
        World world = new World(12345L);
        Player player = new Player(8.0f, 85.0f, 8.0f);
        System.out.println("[VoxelEngine] Engine ready. Starting game loop...");

        double titleUpdateTimer = 0;
        int testFrames = 0;

        // Main game loop
        while (!window.shouldClose()) {
            Time.update();
            float dt = Time.getDeltaTime();

            if (testMode) {
                testFrames++;
                if (testFrames % 10 == 0) {
                    System.out.println("Frame: " + testFrames + " | FPS: " + Time.getFps());
                }
                if (testFrames > 60) {
                    System.out.println("Test mode completed successfully after " + testFrames + " frames.");
                    System.out.println("Final FPS: " + Time.getFps());
                    System.out.println("Loaded chunks: " + world.getLoadedChunksCount());
                    System.out.println("Total sections: " + world.getTotalSectionsCount());
                    System.out.println("Rendered sections: " + world.getRenderedSectionsCount());
                    System.out.println("Frustum culled sections: " + world.getCulledSectionsCount());
                    break;
                }
            }

            // Toggle mouse capture on Escape
            if (Input.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
                Input.setMouseLocked(window.getHandle(), !Input.isMouseLocked());
            }

            // Click window to capture cursor again if unlocked
            if (!Input.isMouseLocked() && Input.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                Input.setMouseLocked(window.getHandle(), true);
            }

            // Benchmark Render Distance controls: '+' / '=' to increase, '-' to decrease
            if (Input.isKeyPressed(GLFW.GLFW_KEY_EQUAL) || Input.isKeyPressed(GLFW.GLFW_KEY_KP_ADD)) {
                world.changeRenderDistance(+1);
            }
            if (Input.isKeyPressed(GLFW.GLFW_KEY_MINUS) || Input.isKeyPressed(GLFW.GLFW_KEY_KP_SUBTRACT)) {
                world.changeRenderDistance(-1);
            }

            // Update player and world
            player.update(world, dt);
            world.update(player.getPosition());
            player.getCamera().update(window.getWidth(), window.getHeight());

            // Clear frame
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // Render voxel world with face culling & frustum culling
            world.render(player.getCamera(), chunkShader, textures);

            // Render wireframe outline around targeted voxel
            if (player.getTargetBlock().hit) {
                outlineRenderer.render(player.getCamera(), player.getTargetBlock().blockPos);
            }

            // Render 2D HUD (Crosshair & Hotbar)
            hudRenderer.render(window.getWidth(), window.getHeight(), player.getHotbarIndex());

            // Update window title with diagnostic metrics
            if (Time.getTime() - titleUpdateTimer >= 0.2) {
                titleUpdateTimer = Time.getTime();
                Vector3f pos = player.getPosition();
                String title = String.format(
                    "%s | FPS: %d | Pos: %.1f, %.1f, %.1f | Chunks: %d (Dist: %d) | Drawn: %d, Culled: %d | %s | [+/- Chunks]",
                    APP_TITLE,
                    Time.getFps(),
                    pos.x, pos.y, pos.z,
                    world.getLoadedChunksCount(),
                    world.getRenderDistance(),
                    world.getRenderedSectionsCount(),
                    world.getCulledSectionsCount(),
                    player.isFlying() ? "[FLYING (Double-Space)]" : "[WALK (Double-Space)]"
                );
                window.setTitle(title);
            }

            // Flush inputs and present frame
            Input.update();
            window.update();
        }

        // Cleanup resources
        outlineRenderer.cleanup();
        hudRenderer.cleanup();
        chunkShader.cleanup();
        textures.cleanup();
        world.cleanup();
        window.destroy();
    }
}
