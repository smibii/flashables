package com.smibii.flashables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.smibii.flashables.client.light.LightRegistry;
import com.smibii.flashables.client.render.shadow.ShadowMapPool;
import com.smibii.flashables.client.render.shadow.ShadowPassRenderer;
import com.smibii.flashables.client.render.shadow.SpotLightShadowMap;
import com.smibii.flashables.light.SpotLight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Draws every {@link SpotLight} currently in the {@link LightRegistry}.
 * Mirrors {@link PointLightRenderer}, but adds cone falloff, a
 * perspective (non-cube) shadow map, and an optional projected
 * "cookie" texture.
 */
public final class SpotLightRenderer {
    private SpotLightRenderer() {}

    public static void renderAll(PoseStack poseStack, float partialTick) {
        ShaderInstance shader = SpotLightShader.SPOT_LIGHT;

        if (shader == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        for (SpotLight light : LightRegistry.getSpotLights()) {
            SceneCopy.copy();

            renderLight(minecraft, poseStack, shader, light);
        }
    }

    private static void renderLight(
            Minecraft minecraft,
            PoseStack poseStack,
            ShaderInstance shader,
            SpotLight light
    ) {
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 position = light.getPosition();
        Vec3 direction = light.getDirection().normalize();

        float lightX = (float) (position.x - camera.x);
        float lightY = (float) (position.y - camera.y);
        float lightZ = (float) (position.z - camera.z);

        Matrix4f modelView = poseStack.last().pose();

        Vector4f lightView = new Vector4f(lightX, lightY, lightZ, 1.0f);
        modelView.transform(lightView);

        Vector4f directionView = new Vector4f((float) direction.x, (float) direction.y, (float) direction.z, 0.0f);
        modelView.transform(directionView);

        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f inverseModelView = new Matrix4f(modelView).invert();
        Matrix4f inverseProjection = new Matrix4f(projection).invert();

        SpotLightShadowMap shadowMap = ShadowMapPool.forSpot(light);

        /*
         * See PointLightRenderer.renderLight() - the shader outputs
         * the fully composited scene color, not a delta, so this
         * needs a hard overwrite. GL blending here made every light
         * after the first render fully black.
         */
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);

        shader.getUniform("ModelViewMat").set(modelView);
        shader.getUniform("ProjMat").set(projection);
        shader.getUniform("InvViewMat").set(inverseModelView);
        shader.getUniform("InvProjMat").set(inverseProjection);
        shader.getUniform("LightShadowMat").set(shadowMap.getShadowMat());
        shader.getUniform("LightPositionView").set(lightView.x, lightView.y, lightView.z);
        shader.getUniform("LightPositionWorld").set((float) position.x, (float) position.y, (float) position.z);
        shader.getUniform("LightDirectionView").set(directionView.x, directionView.y, directionView.z);
        shader.getUniform("LightColor").set(light.getColor().x, light.getColor().y, light.getColor().z);
        shader.getUniform("LightIntensity").set(light.getIntensity());
        shader.getUniform("LightRadius").set(light.getRadius());
        shader.getUniform("LightHasShadows").set(light.isRenderShadows() ? 1.0f : 0.0f);
        shader.getUniform("LightVolumetric").set(light.isRenderVolumetric() ? 1.0f : 0.0f);

        float outerAngle = Math.max(1.0f, light.getAngle());
        /*
         * The cone edge is smoothstep-interpolated between these two
         * angles (see spotFalloff() in the shader), which is smooth
         * in theory - but a band that's only a small fraction of the
         * outer angle fades over so few degrees that it still looks
         * like a hard cutoff. Widen it (at least 6 degrees, or 35% of
         * the outer angle for wide cones) so it's actually visible as
         * a soft edge rather than a step.
         */
        float falloffBand = Math.max(outerAngle * 0.35f, 6.0f);
        float innerAngle = Math.max(outerAngle - falloffBand, 0.0f);
        shader.getUniform("LightAngleOuterCos").set((float) Math.cos(Math.toRadians(outerAngle)));
        shader.getUniform("LightAngleInnerCos").set((float) Math.cos(Math.toRadians(innerAngle)));

        float multiplier = LightEnvironment.getMultiplier(minecraft.level, position, light.getRadius());
        shader.getUniform("LightMultiplier").set(multiplier);
        shader.getUniform("CameraPositionWorld").set((float) camera.x, (float) camera.y, (float) camera.z);
        shader.getUniform("ScreenSize").set((float) minecraft.getWindow().getWidth(), (float) minecraft.getWindow().getHeight());

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(DepthCopy.getTexture());
        shader.setSampler("DepthSampler", DepthCopy.getTexture());

        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        RenderSystem.bindTexture(SceneCopy.getTexture());
        shader.setSampler("SceneSampler", SceneCopy.getTexture());

        RenderSystem.activeTexture(GL13.GL_TEXTURE2);
        RenderSystem.bindTexture(shadowMap.getTexture());
        shader.setSampler("ShadowSampler", shadowMap.getTexture());

        ResourceLocation cookie = light.getTexture();

        if (cookie != null) {
            int cookieTexture = minecraft.getTextureManager().getTexture(cookie).getId();

            RenderSystem.activeTexture(GL13.GL_TEXTURE3);
            RenderSystem.bindTexture(cookieTexture);
            shader.setSampler("ProjectedTexture", cookieTexture);
            shader.getUniform("HasProjectedTexture").set(1.0f);
        } else {
            shader.getUniform("HasProjectedTexture").set(0.0f);
        }

        LightVolumeMesh.renderSphere(poseStack, light.getRadius());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }
}