package com.smibii.flashables.client.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class DepthCopy {
    private static int framebuffer;
    private static int depthTexture;

    private static int width = -1;
    private static int height = -1;

    private DepthCopy() {}

    public static void ensureSize(int width, int height) {
        if (framebuffer != 0 &&
                DepthCopy.width == width &&
                DepthCopy.height == height) {
            return;
        }

        destroy();

        DepthCopy.width = width;
        DepthCopy.height = height;

        /*
         * Depth texture.
         */
        depthTexture = GL11.glGenTextures();

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                depthTexture
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_NEAREST
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_NEAREST
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL14.GL_DEPTH_COMPONENT24,
                width,
                height,
                0,
                GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT,
                0
        );

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                0
        );

        /*
         * Framebuffer containing our depth texture.
         */
        framebuffer =
                GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                framebuffer
        );

        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D,
                depthTexture,
                0
        );

        GL30.glDrawBuffer(GL11.GL_NONE);
        GL30.glReadBuffer(GL11.GL_NONE);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException(
                    "Depth framebuffer incomplete: 0x" + Integer.toHexString(status)
            );
        }

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                0
        );
    }

    public static void copy() {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();

        ensureSize(width, height);
        int mainFramebuffer = minecraft.getMainRenderTarget().frameBufferId;

        /*
         * Main framebuffer → our framebuffer.
         */
        GL30.glBindFramebuffer(
                GL30.GL_READ_FRAMEBUFFER,
                mainFramebuffer
        );

        GL30.glBindFramebuffer(
                GL30.GL_DRAW_FRAMEBUFFER,
                framebuffer
        );

        GL30.glBlitFramebuffer(
                0,
                0,
                width,
                height,

                0,
                0,
                width,
                height,

                GL11.GL_DEPTH_BUFFER_BIT,

                GL11.GL_NEAREST
        );

        /*
         * Restore Minecraft's framebuffer.
         */
        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                mainFramebuffer
        );
    }

    public static int getTexture() {
        return depthTexture;
    }

    public static void destroy() {
        if (depthTexture != 0) {
            GL11.glDeleteTextures(depthTexture);
            depthTexture = 0;
        }

        if (framebuffer != 0) {
            GL30.glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
        }

        width = -1;
        height = -1;
    }
}