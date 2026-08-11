package com.smibii.flashables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.smibii.flashables.client.render.mesh.PointLightMesh;
import com.smibii.flashables.client.render.mesh.SpotLightMesh;
import com.smibii.flashables.light.LightManager;
import com.smibii.flashables.light.types.PointLight;
import com.smibii.flashables.light.types.SpotLight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.List;

public final class DynamicLightingRenderer {
    private static boolean initialized;

    private static PointLightMesh pointMesh;
    private static SpotLightMesh spotMesh;

    private static LightShader pointShader;
    private static LightShader spotShader;

    private DynamicLightingRenderer() {
    }

    public static void init() {

        if (initialized) {
            return;
        }

        RenderSystem.assertOnRenderThread();

        pointMesh =
                new PointLightMesh();

        spotMesh =
                new SpotLightMesh();

        pointShader =
                LightShader.createPointShader();

        spotShader =
                LightShader.createSpotShader();

        LightBuffer.init();

        initialized = true;

        System.out.println(
                "[DynamicLighting] Renderer initialized"
        );
    }

    public static void prepare() {

        if (!initialized) {
            init();
        }

        LightBuffer.update();
    }

    public static void render(
            PoseStack poseStack,
            float partialTick
    ) {

        if (!initialized) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        List<PointLight> lights =
                LightManager.getVisibleLights(
                        minecraft.gameRenderer
                                .getMainCamera()
                                .getPosition(),
                        LightBuffer.MAX_LIGHTS
                );

        if (lights.isEmpty()) {
            return;
        }

        int width =
                minecraft.getWindow()
                        .getWidth();

        int height =
                minecraft.getWindow()
                        .getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        /*
         * Save the current render state.
         */
        RenderSystem.enableBlend();

        /*
         * Lights are additive.
         *
         * Existing framebuffer color +
         * light contribution.
         */
        RenderSystem.blendFunc(
                GL11.GL_ONE,
                GL11.GL_ONE
        );

        /*
         * We don't want the light volume itself to
         * write to the depth buffer.
         */
        RenderSystem.depthMask(false);

        /*
         * We use the existing Minecraft depth buffer.
         */
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        /*
         * We want to render the inside of the light
         * volumes.
         *
         * The camera is normally inside a flashlight's
         * cone / point-light sphere.
         */
        RenderSystem.enableCull();

        GL11.glCullFace(
                GL11.GL_FRONT
        );

        Matrix4f projection =
                RenderSystem.getProjectionMatrix();

        /*
         * Make a copy because invert() modifies
         * the matrix.
         */
        Matrix4f inverseProjection =
                new Matrix4f(projection)
                        .invert();

        /*
         * Minecraft's current model-view matrix.
         */
        Matrix4f view =
                new Matrix4f(
                        RenderSystem.getModelViewMatrix()
                );

        pointShader.bind();

        pointShader.setProjection(
                projection
        );

        pointShader.setInverseProjection(
                inverseProjection
        );

        pointShader.setScreenSize(
                width,
                height
        );

        pointShader.setDepthSampler(0);

        spotShader.bind();

        spotShader.setProjection(
                projection
        );

        spotShader.setInverseProjection(
                inverseProjection
        );

        spotShader.setScreenSize(
                width,
                height
        );

        spotShader.setDepthSampler(0);

        /*
         * Render each light.
         */
        for (PointLight light : lights) {

            if (!light.isEnabled()) {
                continue;
            }

            if (light instanceof SpotLight spot) {

                renderSpotLight(
                        spot,
                        poseStack,
                        view
                );

            } else {

                renderPointLight(
                        light,
                        poseStack,
                        view
                );
            }
        }

        /*
         * Restore state.
         */
        spotShader.unbind();

        DepthBuffer.unbind(0);

        GL11.glCullFace(
                GL11.GL_BACK
        );

        RenderSystem.disableCull();

        RenderSystem.depthMask(true);

        RenderSystem.defaultBlendFunc();

        RenderSystem.disableBlend();
    }

    private static void renderPointLight(
            PointLight light,
            PoseStack poseStack,
            Matrix4f view
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        Vec3 camera =
                minecraft.gameRenderer
                        .getMainCamera()
                        .getPosition();

        Vec3 worldPosition =
                light.getPosition();

        /*
         * Convert world coordinates to coordinates
         * relative to the camera.
         */
        Vec3 relative =
                worldPosition.subtract(camera);

        poseStack.pushPose();

        poseStack.translate(
                relative.x,
                relative.y,
                relative.z
        );

        /*
         * The mesh is a unit sphere.
         */
        poseStack.scale(
                light.getRadius(),
                light.getRadius(),
                light.getRadius()
        );

        Matrix4f modelView =
                new Matrix4f(view)
                        .mul(
                                poseStack.last().pose()
                        );

        pointShader.bind();

        pointShader.setModelView(
                modelView
        );

        /*
         * Light position must be in view space,
         * because the fragment shader reconstructs
         * its scene position in view space.
         */
        Vector4f viewLight =
                new Vector4f(
                        (float) relative.x,
                        (float) relative.y,
                        (float) relative.z,
                        1.0f
                );

        /*
         * 'relative' is already camera-relative,
         * and the view matrix is needed for rotation.
         */
        Vector4f transformed =
                new Vector4f(
                        (float) relative.x,
                        (float) relative.y,
                        (float) relative.z,
                        1.0f
                );

        view.transform(
                transformed
        );

        pointShader.setLightPosition(
                transformed.x,
                transformed.y,
                transformed.z
        );

        pointShader.setLightColor(
                light.getColor().x(),
                light.getColor().y(),
                light.getColor().z()
        );

        pointShader.setIntensity(
                light.getIntensity()
        );

        pointShader.setRadius(
                light.getRadius()
        );

        pointMesh.bind();

        pointMesh.draw();

        pointMesh.unbind();

        poseStack.popPose();
    }

    private static void renderSpotLight(
            SpotLight light,
            PoseStack poseStack,
            Matrix4f view
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        Vec3 camera =
                minecraft.gameRenderer
                        .getMainCamera()
                        .getPosition();

        Vec3 worldPosition =
                light.getPosition();

        Vec3 relative =
                worldPosition.subtract(camera);

        Vec3 direction =
                light.getDirection()
                        .normalize();

        poseStack.pushPose();

        poseStack.translate(
                relative.x,
                relative.y,
                relative.z
        );

        /*
         * Rotate +Z into the flashlight direction.
         */
        poseStack.mulPose(
                new org.joml.Quaternionf()
                        .rotationTo(
                                new org.joml.Vector3f(
                                        0,
                                        0,
                                        1
                                ),
                                new org.joml.Vector3f(
                                        (float) direction.x,
                                        (float) direction.y,
                                        (float) direction.z
                                )
                        )
        );

        /*
         * Cone has:
         *
         * depth = 1
         * radius = 1
         *
         * Scale it to:
         *
         * depth = light radius
         * radius = tan(angle) * radius
         */
        float radius =
                light.getRadius();

        float coneRadius =
                (float)
                        Math.tan(
                                Math.toRadians(
                                        light.getOuterAngle()
                                )
                        ) *
                        radius;

        poseStack.scale(
                coneRadius,
                coneRadius,
                radius
        );

        Matrix4f modelView =
                new Matrix4f(view)
                        .mul(
                                poseStack.last().pose()
                        );

        Vector4f transformedPosition =
                new Vector4f(
                        (float) relative.x,
                        (float) relative.y,
                        (float) relative.z,
                        1.0f
                );

        view.transform(
                transformedPosition
        );

        /*
         * Transform the direction into view space.
         */
        Vector4f transformedDirection =
                new Vector4f(
                        (float) direction.x,
                        (float) direction.y,
                        (float) direction.z,
                        0.0f
                );

        view.transform(
                transformedDirection
        );

        spotShader.bind();

        spotShader.setModelView(
                modelView
        );

        spotShader.setLightPosition(
                transformedPosition.x,
                transformedPosition.y,
                transformedPosition.z
        );

        spotShader.setLightDirection(
                transformedDirection.x,
                transformedDirection.y,
                transformedDirection.z
        );

        spotShader.setLightColor(
                light.getColor().x(),
                light.getColor().y(),
                light.getColor().z()
        );

        spotShader.setIntensity(
                light.getIntensity()
        );

        spotShader.setRadius(
                light.getRadius()
        );

        spotShader.setInnerCos(
                light.getInnerCos()
        );

        spotShader.setOuterCos(
                light.getOuterCos()
        );

        /*
         * Texture support.
         *
         * For now this binds the flashlight texture
         * through Minecraft's texture manager.
         */
        if (light.getTexture() != null) {

            int textureId =
                    minecraft.getTextureManager()
                            .getTexture(
                                    light.getTexture()
                            )
                            .getId();

            GL13.glActiveTexture(
                    GL13.GL_TEXTURE1
            );

            GL11.glBindTexture(
                    GL11.GL_TEXTURE_2D,
                    textureId
            );

            spotShader.setLightTexture(1);
        }

        spotMesh.bind();

        spotMesh.draw();

        spotMesh.unbind();

        poseStack.popPose();
    }

    public static void shutdown() {

        if (!initialized) {
            return;
        }

        RenderSystem.assertOnRenderThread();

        if (pointMesh != null) {
            pointMesh.destroy();
        }

        if (spotMesh != null) {
            spotMesh.destroy();
        }

        if (pointShader != null) {
            pointShader.destroy();
        }

        if (spotShader != null) {
            spotShader.destroy();
        }

        LightBuffer.destroy();

        initialized = false;
    }
}