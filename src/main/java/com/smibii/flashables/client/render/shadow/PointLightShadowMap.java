package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.nio.IntBuffer;

/**
 * Six-face depth cubemap used for a single point light's shadows.
 * Every point light in the {@link com.smibii.flashables.client.light.LightRegistry}
 * owns its own instance (see {@link ShadowMapPool}), since shadow maps
 * for every light must exist at once: they're all rendered up front,
 * in {@link ShadowPassRenderer}, before the frame's normal level
 * render begins, then just sampled while drawing each light's volume.
 * Unlike the spot light map, no shadow-space matrix is exposed: the
 * point light fragment shader samples this cubemap directly by
 * direction from the light instead of a projected UV.
 * <p>
 * Each face's depth content comes from {@link ShadowGeometryRenderer}
 * (solid block geometry we build and draw ourselves), not from asking
 * Minecraft to render the level - see {@link #render} for why.
 */
public class PointLightShadowMap {
    public static final int SIZE = 1024;

    private static final float NEAR_PLANE = 0.05f;

    private int framebuffer = -1;
    private int depthCubemap = -1;

    private boolean initialized = false;
    private boolean rendering = false;

    public boolean isRendering() {
        return rendering;
    }

    public void init() {

        if (initialized) {
            return;
        }

        if (!RenderSystem.isOnRenderThread()) {

            RenderSystem.recordRenderCall(
                    this::init
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

    /**
     * Renders the six-face depth cubemap for this point light. Must
     * only be called from {@link ShadowPassRenderer}, before the
     * frame's normal level render starts.
     */
    public void render(Vec3 light, float radius) {
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
            /*
             * ------------------------------------------------
             * Save OpenGL state
             * ------------------------------------------------
             */

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

            /*
             * ------------------------------------------------
             * Render all six cubemap faces
             * ------------------------------------------------
             *
             * Face index N = GL_TEXTURE_CUBE_MAP_POSITIVE_X + N, in
             * OpenGL's fixed order: +X, -X, +Y, -Y, +Z, -Z. The
             * yaw/pitch below must make Minecraft's view-vector
             * formula (direction.x = -sin(yaw)*cos(pitch),
             * direction.y = -sin(pitch), direction.z = cos(yaw)*cos(pitch))
             * point each face in that same direction, or content ends
             * up stored in the wrong face and every shadow/cookie
             * lookup along that axis samples the wrong data.
             */

            renderFace(minecraft, light, projection, radius, 0, -90.0f, 0.0f); // +X
            renderFace(minecraft, light, projection, radius, 1, 90.0f, 0.0f);  // -X
            renderFace(minecraft, light, projection, radius, 2, 0.0f, -90.0f); // +Y
            renderFace(minecraft, light, projection, radius, 3, 0.0f, 90.0f);  // -Y
            renderFace(minecraft, light, projection, radius, 4, 0.0f, 0.0f);   // +Z
            renderFace(minecraft, light, projection, radius, 5, 180.0f, 0.0f); // -Z

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

    private void renderFace(
            Minecraft minecraft,
            Vec3 light,
            Matrix4f projection,
            float radius,
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
         * Same yaw/pitch-to-rotation convention as
         * SpotLightShadowMap uses for its shadow matrix - matches
         * the direction each cubemap face is expected to look, which
         * is what the fragment shader's cubemap sampling relies on.
         */

        Matrix4f view =
                new Matrix4f()
                        .rotateX((float) Math.toRadians(pitch))
                        .rotateY((float) Math.toRadians(yaw + 180.0f));

        ShadowGeometryRenderer.render(minecraft.level, light, radius, view, projection);
    }

    public int getTexture() {
        return depthCubemap;
    }

    public void destroy() {

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
}