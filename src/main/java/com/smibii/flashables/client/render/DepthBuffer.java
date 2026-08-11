package com.smibii.flashables.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class DepthBuffer {
    private DepthBuffer() {
    }

    public static int getTextureId() {

        Minecraft minecraft =
                Minecraft.getInstance();

        RenderTarget target =
                minecraft.getMainRenderTarget();

        return target.getDepthTextureId();
    }

    public static void bind(
            int textureUnit
    ) {

        int texture =
                getTextureId();

        if (texture == 0) {
            return;
        }

        GL13.glActiveTexture(
                GL13.GL_TEXTURE0 +
                        textureUnit
        );

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                texture
        );
    }

    public static void unbind(
            int textureUnit
    ) {

        GL13.glActiveTexture(
                GL13.GL_TEXTURE0 +
                        textureUnit
        );

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                0
        );
    }
}
