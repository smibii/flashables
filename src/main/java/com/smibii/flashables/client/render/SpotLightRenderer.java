package com.smibii.flashables.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.smibii.flashables.client.light.LightRegistry;
import com.smibii.flashables.light.SpotLight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
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
        Matrix4f lightProjectionMat = computeLightProjectionMat(direction, light.getAngle(), light.getRadius());

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

        setUniform(shader, "ModelViewMat", modelView);
        setUniform(shader, "ProjMat", projection);
        setUniform(shader, "InvViewMat", inverseModelView);
        setUniform(shader, "InvProjMat", inverseProjection);
        setUniform(shader, "LightProjectionMat", lightProjectionMat);
        setUniform(shader, "LightPositionView", lightView.x, lightView.y, lightView.z);
        setUniform(shader, "LightPositionWorld", (float) position.x, (float) position.y, (float) position.z);
        setUniform(shader, "LightDirectionView", directionView.x, directionView.y, directionView.z);
        setUniform(shader, "LightColor", light.getColor().x, light.getColor().y, light.getColor().z);
        setUniform(shader, "LightIntensity", light.getIntensity());
        setUniform(shader, "LightRadius", light.getRadius());
        setUniform(shader, "LightHasShadows", light.isRenderShadows() ? 1.0f : 0.0f);
        setUniform(shader, "LightVolumetric", light.isRenderVolumetric() ? 1.0f : 0.0f);
        setUniform(shader, "LightVolumetricStrength", light.getVolumetricStrength());
        setUniform(shader, "LightVolumetricStep", light.getVolumetricStep());
        setUniform(shader, "LightVolumetricRenderDistance", 200.0f);

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
        setUniform(shader, "LightAngleOuterCos", (float) Math.cos(Math.toRadians(outerAngle)));
        setUniform(shader, "LightAngleInnerCos", (float) Math.cos(Math.toRadians(innerAngle)));

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

        ResourceLocation cookie = light.getTexture();

        if (cookie != null) {
            int cookieTexture = minecraft.getTextureManager().getTexture(cookie).getId();

            RenderSystem.activeTexture(GL13.GL_TEXTURE3);
            RenderSystem.bindTexture(cookieTexture);
            shader.setSampler("ProjectedTexture", cookieTexture);
            setUniform(shader, "HasProjectedTexture", 1.0f);
        } else {
            setUniform(shader, "HasProjectedTexture", 0.0f);
        }

        LightVolumeMesh.renderSphere(poseStack, light.getRadius());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static Matrix4f computeLightProjectionMat(Vec3 direction, float angleDegrees, float radius) {
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        float fov = Math.min(angleDegrees * 2.0f + 4.0f, 170.0f);
        float nearPlane = 0.05f;
        float far = Math.max(radius, nearPlane + 0.1f);

        Matrix4f projection = new Matrix4f()
                .perspective((float) Math.toRadians(fov), 1.0f, nearPlane, far);

        Matrix4f view = new Matrix4f()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw + 180.0f));

        return projection.mul(view);
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