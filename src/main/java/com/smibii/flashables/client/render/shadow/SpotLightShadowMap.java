package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

/**
 * Single-frustum depth map used for a single spot light's shadows
 * and, when that light has a projected texture, for sampling that
 * texture's UVs. Every spot light in the
 * {@link com.smibii.flashables.client.light.LightRegistry} owns its
 * own instance (see {@link ShadowMapPool}) - all of them are rendered
 * up front, in {@link ShadowPassRenderer}, before the frame's normal
 * level render begins, then just sampled while drawing each light's
 * volume, the same way {@link PointLightShadowMap} does for point
 * lights.
 * <p>
 * The depth content comes from {@link ShadowGeometryRenderer} (solid
 * block geometry we build and draw ourselves), not from asking
 * Minecraft to render the level - see {@link PointLightShadowMap#render}
 * for why.
 */
public class SpotLightShadowMap {
    public static final int SIZE = 1024;

    private static final float NEAR_PLANE = 0.05f;

    private int framebuffer = -1;
    private int depthTexture = -1;

    private boolean initialized = false;
    private boolean rendering = false;

    private final Matrix4f lastShadowMat = new Matrix4f();

    public boolean isRendering() {
        return rendering;
    }

    public void init() {

        if (initialized) {
            return;
        }

        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::init);
            return;
        }

        depthTexture = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL14.GL_DEPTH_COMPONENT24,
                SIZE,
                SIZE,
                0,
                GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT,
                0
        );

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        framebuffer = GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D,
                depthTexture,
                0
        );

        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException(
                    "Spot light shadow framebuffer incomplete: 0x" + Integer.toHexString(status)
            );
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        initialized = true;
    }

    /**
     * Renders this spot light's depth map. Must only be called from
     * {@link ShadowPassRenderer}, before the frame's normal level
     * render starts - see {@link PointLightShadowMap#render} for why.
     */
    public void render(
            Vec3 lightPosition,
            Vec3 direction,
            float angleDegrees,
            float radius
    ) {
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
            int oldFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

            IntBuffer viewport = BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

            int oldViewportX = viewport.get(0);
            int oldViewportY = viewport.get(1);
            int oldViewportWidth = viewport.get(2);
            int oldViewportHeight = viewport.get(3);

            boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL11.glViewport(0, 0, SIZE, SIZE);

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();

            GL11.glColorMask(false, false, false, false);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

            Vec3 forward = direction.normalize();
            float pitch = (float) Math.toDegrees(Math.asin(-forward.y));
            float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));

            float fov = Math.min(angleDegrees * 2.0f + 4.0f, 170.0f);
            float far = Math.max(radius, NEAR_PLANE + 0.1f);

            Matrix4f projection = new Matrix4f()
                    .perspective((float) Math.toRadians(fov), 1.0f, NEAR_PLANE, far);

            Matrix4f view = new Matrix4f()
                    .rotateX((float) Math.toRadians(pitch))
                    .rotateY((float) Math.toRadians(yaw + 180.0f));

            ShadowGeometryRenderer.render(minecraft.level, lightPosition, radius, view, projection);

            lastShadowMat.set(projection).mul(view);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, oldFramebuffer);
            GL11.glViewport(oldViewportX, oldViewportY, oldViewportWidth, oldViewportHeight);
            GL11.glColorMask(true, true, true, true);

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

    /**
     * Recomputes {@link #getShadowMat()} from the light's transform
     * alone, without touching the depth texture or the GPU. Used by
     * {@link ShadowPassRenderer} for lights that have a projected
     * "cookie" texture but shadows turned off - the cookie only needs
     * this matrix, not real shadow occlusion, so there's no reason to
     * pay for a full {@link #render} on those.
     */
    public void updateShadowMatOnly(Vec3 direction, float angleDegrees, float radius) {
        Vec3 forward = direction.normalize();
        float pitch = (float) Math.toDegrees(Math.asin(-forward.y));
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));

        float fov = Math.min(angleDegrees * 2.0f + 4.0f, 170.0f);
        float far = Math.max(radius, NEAR_PLANE + 0.1f);

        Matrix4f projection = new Matrix4f()
                .perspective((float) Math.toRadians(fov), 1.0f, NEAR_PLANE, far);

        Matrix4f view = new Matrix4f()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw + 180.0f));

        lastShadowMat.set(projection).mul(view);
    }

    public int getTexture() {
        return depthTexture;
    }

    public Matrix4f getShadowMat() {
        return lastShadowMat;
    }

    public void destroy() {

        if (!initialized) {
            return;
        }

        if (depthTexture != -1) {
            GL11.glDeleteTextures(depthTexture);
            depthTexture = -1;
        }

        if (framebuffer != -1) {
            GL30.glDeleteFramebuffers(framebuffer);
            framebuffer = -1;
        }

        initialized = false;
    }
}