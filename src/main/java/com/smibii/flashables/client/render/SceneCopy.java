package com.smibii.flashables.client.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class SceneCopy {

    private static int framebuffer;
    private static int texture;

    private static int width = -1;
    private static int height = -1;

    private SceneCopy() {
    }

    public static void ensureSize(int width, int height) {

        if (framebuffer != 0
                && SceneCopy.width == width
                && SceneCopy.height == height) {
            return;
        }

        destroy();

        SceneCopy.width = width;
        SceneCopy.height = height;

        /*
         * Create RGBA color texture.
         */
        texture = GL11.glGenTextures();

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                texture
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
                GL11.GL_CLAMP
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T,
                GL11.GL_CLAMP
        );

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                width,
                height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                0
        );

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                0
        );

        /*
         * Create framebuffer.
         */
        framebuffer =
                GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                framebuffer
        );

        /*
         * Attach color texture.
         */
        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D,
                texture,
                0
        );

        /*
         * We don't need depth in this framebuffer.
         */
        GL30.glDrawBuffer(
                GL30.GL_COLOR_ATTACHMENT0
        );

        GL30.glReadBuffer(
                GL30.GL_COLOR_ATTACHMENT0
        );

        int status =
                GL30.glCheckFramebufferStatus(
                        GL30.GL_FRAMEBUFFER
                );

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {

            throw new IllegalStateException(
                    "SceneCopy framebuffer incomplete: 0x"
                            + Integer.toHexString(status)
            );
        }

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                0
        );
    }

    public static void copy() {

        Minecraft minecraft =
                Minecraft.getInstance();

        int width =
                minecraft
                        .getWindow()
                        .getWidth();

        int height =
                minecraft
                        .getWindow()
                        .getHeight();

        ensureSize(width, height);

        int mainFramebuffer =
                minecraft
                        .getMainRenderTarget()
                        .frameBufferId;

        /*
         * Source:
         * Minecraft's framebuffer.
         *
         * Destination:
         * Our RGBA8 texture framebuffer.
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

                GL11.GL_COLOR_BUFFER_BIT,

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
        return texture;
    }

    public static void destroy() {

        if (texture != 0) {
            GL11.glDeleteTextures(texture);
            texture = 0;
        }

        if (framebuffer != 0) {
            GL30.glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
        }

        width = -1;
        height = -1;
    }
}