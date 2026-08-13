package com.smibii.flashables.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.smibii.flashables.client.light.LightRegistry;
import com.smibii.flashables.light.PointLight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;

/**
 * Draws every {@link PointLight} currently in the {@link LightRegistry}.
 * Invoked by {@link LightingRenderer}, once per frame, after the depth
 * buffer has already been snapshotted for the frame.
 */
public final class PointLightRenderer {
    private PointLightRenderer() {}

    public static void renderAll(PoseStack poseStack, float partialTick) {
        ShaderInstance shader = PointLightShader.POINT_LIGHT;

        if (shader == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        for (PointLight light : LightRegistry.getPointLights()) {
            /*
             * Re-snapshot the scene so this light's contribution
             * builds on top of every light rendered before it.
             */
            SceneCopy.copy();

            renderLight(minecraft, poseStack, shader, light);
        }
    }

    private static void renderLight(
            Minecraft minecraft,
            PoseStack poseStack,
            ShaderInstance shader,
            PointLight light
    ) {
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 position = light.getPosition();

        float lightX = (float) (position.x - camera.x);
        float lightY = (float) (position.y - camera.y);
        float lightZ = (float) (position.z - camera.z);

        Matrix4f modelView = poseStack.last().pose();
        Vector4f lightView = new Vector4f(lightX, lightY, lightZ, 1.0f);
        modelView.transform(lightView);

        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f inverseModelView = new Matrix4f(modelView).invert();
        Matrix4f inverseProjection = new Matrix4f(projection).invert();

        /*
         * The shader reads the pre-light scene from SceneSampler and
         * outputs the fully composited result, not a delta to blend -
         * so this needs a hard overwrite, not GL blending. Blending
         * (GL_SRC_ALPHA_SATURATE against dst alpha) used to zero out
         * every light after the first: our shader always outputs
         * alpha 1.0, so once one light had written a pixel, dst alpha
         * was already 1.0 and the src/dst blend factors both
         * collapsed to 0 - every subsequent light drawn over the same
         * pixels came out fully black.
         */
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);

        setUniform(shader, "ModelViewMat", modelView);
        setUniform(shader, "ProjMat", projection);
        setUniform(shader, "InvViewMat", inverseModelView);
        setUniform(shader, "InvProjMat", inverseProjection);
        setUniform(shader, "LightPositionView", lightView.x, lightView.y, lightView.z);
        setUniform(shader, "LightPositionWorld", (float) position.x, (float) position.y, (float) position.z);
        setUniform(shader, "LightColor", light.getColor().x, light.getColor().y, light.getColor().z);
        setUniform(shader, "LightIntensity", light.getIntensity());
        setUniform(shader, "LightRadius", light.getRadius());
        setUniform(shader, "LightHasShadows", light.isRenderShadows() ? 1.0f : 0.0f);
        setUniform(shader, "LightVolumetric", light.isRenderVolumetric() ? 1.0f : 0.0f);
        setUniform(shader, "LightVolumetricStrength", light.getVolumetricStrength());
        setUniform(shader, "LightVolumetricStep", light.getVolumetricStep());
        setUniform(shader, "LightVolumetricRenderDistance", 200.0f);

        float multiplier = LightEnvironment.getMultiplier(minecraft.level, position, light.getRadius());
        setUniform(shader, "LightMultiplier", multiplier);
        setUniform(shader, "CameraPositionWorld", (float) camera.x, (float) camera.y, (float) camera.z);
        setUniform(shader, "ScreenSize", (float) minecraft.getWindow().getWidth(), (float) minecraft.getWindow().getHeight());

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(DepthCopy.getTexture());
        shader.setSampler("DepthSampler", DepthCopy.getTexture());

        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        RenderSystem.bindTexture(SceneCopy.getTexture());
        shader.setSampler("SceneSampler", SceneCopy.getTexture());

        LightVolumeMesh.renderSphere(poseStack, light.getRadius());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void setUniform(ShaderInstance shader, String name, Matrix4f value) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setUniform(ShaderInstance shader, String name, float x) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(x);
        }
    }

    private static void setUniform(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void setUniform(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }
}