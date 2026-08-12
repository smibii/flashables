package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.smibii.flashables.client.render.PointLightRenderer;
import com.smibii.flashables.light.Light;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.nio.IntBuffer;

public class PointLightShadowMap {
    public static final int SIZE = 1024;

    private static final float NEAR_PLANE = 0.05f;

    private static int framebuffer = -1;
    private static int depthCubemap = -1;

    private static boolean initialized = false;

    private static boolean rendering = false;

    public static boolean isRendering() {
        return rendering;
    }

    private static final ShadowCamera SHADOW_CAMERA =
            new ShadowCamera();

    private PointLightShadowMap() {}

    public static void init() {

        if (initialized) {
            return;
        }

        if (!RenderSystem.isOnRenderThread()) {

            RenderSystem.recordRenderCall(
                    PointLightShadowMap::init
            );

            return;
        }

        /*
         * ------------------------------------------------
         * Create depth cubemap
         * ------------------------------------------------
         */

        depthCubemap =
                GL11.glGenTextures();

        GL11.glBindTexture(
                GL32.GL_TEXTURE_CUBE_MAP,
                depthCubemap
        );

        for (int face = 0; face < 6; face++) {

            GL11.glTexImage2D(
                    GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                    0,
                    GL30.GL_DEPTH_COMPONENT24,
                    SIZE,
                    SIZE,
                    0,
                    GL11.GL_DEPTH_COMPONENT,
                    GL11.GL_UNSIGNED_INT,
                    0
            );
        }

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_LINEAR
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_LINEAR
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_WRAP_S,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_WRAP_T,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL12.GL_TEXTURE_WRAP_R,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glBindTexture(
                GL32.GL_TEXTURE_CUBE_MAP,
                0
        );

        /*
         * ------------------------------------------------
         * Framebuffer
         * ------------------------------------------------
         */

        framebuffer =
                GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                framebuffer
        );

        /*
         * Attach the first face initially.
         * renderFace() changes the attachment for
         * every cubemap direction.
         */

        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X,
                depthCubemap,
                0
        );

        GL11.glDrawBuffer(
                GL11.GL_NONE
        );

        GL11.glReadBuffer(
                GL11.GL_NONE
        );

        int status =
                GL30.glCheckFramebufferStatus(
                        GL30.GL_FRAMEBUFFER
                );

        if (status !=
                GL30.GL_FRAMEBUFFER_COMPLETE) {

            throw new IllegalStateException(
                    "Point light shadow framebuffer incomplete: "
                            + status
            );
        }

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                0
        );

        initialized = true;
    }

    public static void render(
            Minecraft minecraft,
            Light<?> light
    ) {

        /*
         * This overload isn't compatible with your
         * current PointLightRenderer because PointLightTest
         * is not nested there.
         *
         * Keep the actual render() below.
         */
    }

    public static void render(float partialTick) {
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }

        if (rendering) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (!initialized) {
            init();
        }

        rendering = true;

        try {
            Vec3 light =
                    PointLightRenderer.LIGHT
                            .getPosition();

            float radius =
                    PointLightRenderer.LIGHT
                            .getRadius();

            /*
             * ------------------------------------------------
             * Save OpenGL state
             * ------------------------------------------------
             */

            Matrix4f oldProjection =
                    new Matrix4f(
                            RenderSystem.getProjectionMatrix()
                    );

            int oldFramebuffer =
                    GL11.glGetInteger(
                            GL30.GL_FRAMEBUFFER_BINDING
                    );

            /*
             * glGetInteger cannot read individual viewport
             * components. Use glGetIntegerv().
             */

            IntBuffer viewport =
                    BufferUtils.createIntBuffer(4);

            GL11.glGetIntegerv(
                    GL11.GL_VIEWPORT,
                    viewport
            );

            int oldViewportX =
                    viewport.get(0);

            int oldViewportY =
                    viewport.get(1);

            int oldViewportWidth =
                    viewport.get(2);

            int oldViewportHeight =
                    viewport.get(3);

            boolean depthTest =
                    GL11.glIsEnabled(
                            GL11.GL_DEPTH_TEST
                    );

            boolean cull =
                    GL11.glIsEnabled(
                            GL11.GL_CULL_FACE
                    );

            boolean blend =
                    GL11.glIsEnabled(
                            GL11.GL_BLEND
                    );

            /*
             * ------------------------------------------------
             * Bind shadow framebuffer
             * ------------------------------------------------
             */

            GL30.glBindFramebuffer(
                    GL30.GL_FRAMEBUFFER,
                    framebuffer
            );

            GL11.glViewport(
                    0,
                    0,
                    SIZE,
                    SIZE
            );

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            RenderSystem.disableBlend();
            RenderSystem.enableCull();

            /*
             * We only render depth.
             */

            GL11.glColorMask(
                    false,
                    false,
                    false,
                    false
            );

            GameRenderer gameRenderer =
                    minecraft.gameRenderer;

            LightTexture lightTexture =
                    gameRenderer.lightTexture();

            /*
             * ------------------------------------------------
             * 90 degree projection
             * ------------------------------------------------
             */

            Matrix4f projection =
                    new Matrix4f()
                            .perspective(
                                    (float)
                                            Math.toRadians(
                                                    90.0
                                            ),
                                    1.0f,
                                    NEAR_PLANE,
                                    radius
                            );

            RenderSystem.setProjectionMatrix(
                    projection,
                    VertexSorting.DISTANCE_TO_ORIGIN
            );

            /*
             * ------------------------------------------------
             * Render all six cubemap faces
             * ------------------------------------------------
             *
             * OpenGL cubemap directions:
             *
             * +X
             * -X
             * +Y
             * -Y
             * +Z
             * -Z
             */

            renderFace(
                    minecraft,
                    light,
                    lightTexture,
                    gameRenderer,
                    projection,
                    partialTick,
                    0,
                    90.0f,
                    0.0f
            );

            renderFace(
                    minecraft,
                    light,
                    lightTexture,
                    gameRenderer,
                    projection,
                    partialTick,
                    1,
                    -90.0f,
                    0.0f
            );

            renderFace(
                    minecraft,
                    light,
                    lightTexture,
                    gameRenderer,
                    projection,
                    partialTick,
                    2,
                    0.0f,
                    -90.0f
            );

            renderFace(
                    minecraft,
                    light,
                    lightTexture,
                    gameRenderer,
                    projection,
                    partialTick,
                    3,
                    0.0f,
                    90.0f
            );

            renderFace(
                    minecraft,
                    light,
                    lightTexture,
                    gameRenderer,
                    projection,
                    partialTick,
                    4,
                    0.0f,
                    0.0f
            );

            renderFace(
                    minecraft,
                    light,
                    lightTexture,
                    gameRenderer,
                    projection,
                    partialTick,
                    5,
                    180.0f,
                    0.0f
            );

            /*
             * ------------------------------------------------
             * Restore framebuffer
             * ------------------------------------------------
             */

            GL30.glBindFramebuffer(
                    GL30.GL_FRAMEBUFFER,
                    oldFramebuffer
            );

            GL11.glViewport(
                    oldViewportX,
                    oldViewportY,
                    oldViewportWidth,
                    oldViewportHeight
            );

            GL11.glColorMask(
                    true,
                    true,
                    true,
                    true
            );

            RenderSystem.setProjectionMatrix(
                    oldProjection,
                    VertexSorting.DISTANCE_TO_ORIGIN
            );

            /*
             * Restore GL state.
             */

            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }

            if (cull) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }

            if (blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }

            RenderSystem.depthMask(true);
        } finally {
            rendering = false;
        }
    }

    private static void renderFace(
            Minecraft minecraft,
            Vec3 light,
            LightTexture lightTexture,
            GameRenderer gameRenderer,
            Matrix4f projection,
            float partialTick,
            int face,
            float yaw,
            float pitch
    ) {

        /*
         * Attach the correct cubemap face.
         */

        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                depthCubemap,
                0
        );

        GL11.glDrawBuffer(
                GL11.GL_NONE
        );

        GL11.glReadBuffer(
                GL11.GL_NONE
        );

        GL11.glClear(
                GL11.GL_DEPTH_BUFFER_BIT
        );

        /*
         * Configure our custom camera.
         */

        SHADOW_CAMERA.setup(
                minecraft.level,
                minecraft.player,
                false,
                false,
                partialTick
        );

        SHADOW_CAMERA.setPosition(
                light.x,
                light.y,
                light.z
        );

        SHADOW_CAMERA.setRotation(
                yaw,
                pitch
        );

        /*
         * Tell Minecraft's entity renderer which
         * camera is currently rendering.
         */

        minecraft.getEntityRenderDispatcher()
                .prepare(
                        minecraft.level,
                        SHADOW_CAMERA,
                        minecraft.player
                );

        /*
         * Render the level from the light.
         */

        PoseStack poseStack =
                new PoseStack();

        minecraft.levelRenderer.renderLevel(
                poseStack,
                partialTick,
                minecraft.level.getGameTime(),
                false,
                SHADOW_CAMERA,
                gameRenderer,
                lightTexture,
                projection
        );
    }

    public static int getTexture() {

        return depthCubemap;
    }

    public static void destroy() {

        if (!initialized) {
            return;
        }

        if (depthCubemap != -1) {

            GL11.glDeleteTextures(
                    depthCubemap
            );

            depthCubemap = -1;
        }

        if (framebuffer != -1) {

            GL30.glDeleteFramebuffers(
                    framebuffer
            );

            framebuffer = -1;
        }

        initialized = false;
    }

    /*
     * Custom camera used exclusively for shadow rendering.
     */
    private static class ShadowCamera extends Camera {
        public void setPosition(
                double x,
                double y,
                double z
        ) {
            super.setPosition(x, y, z);
        }

        public void setRotation(
                float yaw,
                float pitch
        ) {
            super.setRotation(yaw, pitch);
        }
    }
}
