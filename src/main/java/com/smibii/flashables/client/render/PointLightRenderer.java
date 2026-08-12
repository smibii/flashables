package com.smibii.flashables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.smibii.flashables.helper.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PointLightRenderer {
    public static final PointLightTest LIGHT = new PointLightTest();

    private PointLightRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        ShaderInstance shader = PointLightShader.POINT_LIGHT;

        SceneCopy.copy();
        DepthCopy.copy();

        renderLight(event.getPoseStack(), shader);
    }

    private static float getEasedTimeFactor(long worldTime) {
        long timeOfDay = worldTime % 24000;
        float progress = 0.0f;

        if (timeOfDay >= 12000 && timeOfDay <= 13000) {
            // 🌇 SUNSET: From start of twilight (12000) to full dark (13000)
            progress = (timeOfDay - 12000) / 1000.0f; // Smoothly ascends 0.0 -> 1.0
        }
        else if (timeOfDay > 13000 && timeOfDay < 23000) {
            // 🌙 NIGHTTIME: Completely dark
            progress = 1.0f;
        }
        else if (timeOfDay >= 21000 && timeOfDay <= 24000) {
            // 🌅 SUNRISE: From start of morning light (23000) to full brightness (24000)
            progress = (24000 - timeOfDay) / 3000.0f; // Smoothly descends 1.0 -> 0.0
        }
        else {
            // ☀️ DAYTIME: Completely light
            progress = 0.0f;
        }

        // Linearly scales the progress directly into your 1.0 to 5.0 range
        return 1.0f + (progress * 4.0f);
    }

    private static float easeInOutCubic(float x) {
        float sharpness = 100.0f;

        float n = (x - 0.5f) * sharpness;
        float sigmoid = n / (float) Math.sqrt(1.0f + n * n);

        return (sigmoid + 1.0f) / 2.0f;
    }

    private static boolean isSkyExposed(Level level, BlockPos pos, int maxDistance) {
        for (int y = pos.getY() + 1;
             y <= pos.getY() + maxDistance && y < level.getMaxBuildHeight();
             y++) {

            BlockPos above = new BlockPos(
                    pos.getX(),
                    y,
                    pos.getZ()
            );

            if (!level.getBlockState(above).getBlock().defaultBlockState()
                    .isAir()) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasNearbySkyLight(Level level, BlockPos pos, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                BlockPos check = pos.offset(x, 0, z);

                int skyLight = level.getBrightness(
                        LightLayer.SKY,
                        check
                );

                if (skyLight >= 12) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void renderLight(
            PoseStack poseStack,
            ShaderInstance shader
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        Vec3 camera =
                minecraft.gameRenderer
                        .getMainCamera()
                        .getPosition();

        Vec3 light =
                LIGHT.getPosition();

        /*
         * Camera-relative light position.
         */
        float lightX =
                (float) (light.x - camera.x);

        float lightY =
                (float) (light.y - camera.y);

        float lightZ =
                (float) (light.z - camera.z);

        /*
         * This is the exact matrix used to transform
         * our sphere vertices.
         */
        Matrix4f modelView =
                poseStack.last().pose();

        /*
         * Convert the light position into the same
         * view space used by the depth reconstruction.
         */
        Vector4f lightView =
                new Vector4f(
                        lightX,
                        lightY,
                        lightZ,
                        1.0f
                );

        modelView.transform(lightView);

        Matrix4f projection =
                RenderSystem.getProjectionMatrix();

        Matrix4f inverseProjection =
                new Matrix4f(projection)
                        .invert();

        RenderSystem.enableBlend();

        RenderSystem.blendFunc(
                GL11.GL_SRC_ALPHA_SATURATE,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );

        /*
         * We don't want the sphere itself to depth-test.
         * The depth texture does the surface mapping.
         */
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.disableCull();

        RenderSystem.setShader(
                () -> shader
        );

        /*
         * Vertex shader.
         */
        shader.getUniform(
                "ModelViewMat"
        ).set(modelView);

        shader.getUniform(
                "ProjMat"
        ).set(projection);

        /*
         * Fragment reconstruction.
         */
        shader.getUniform(
                "InvProjMat"
        ).set(inverseProjection);

        /*
         * Light in VIEW SPACE.
         */
        shader.getUniform(
                "LightPositionView"
        ).set(
                lightView.x,
                lightView.y,
                lightView.z
        );

        shader.getUniform(
                "LightColor"
        ).set(
                LIGHT.getColor().x,
                LIGHT.getColor().y,
                LIGHT.getColor().z
        );

        shader.getUniform(
                "LightIntensity"
        ).set(
                LIGHT.getIntensity()
        );

        shader.getUniform(
                "LightRadius"
        ).set(
                LIGHT.getRadius()
        );

        float environmentMultiplier = getEasedTimeFactor(minecraft.level.dayTime());
        int blockLight =
                minecraft.level.getBrightness(
                        LightLayer.BLOCK,
                        new BlockPos((int) light.x, (int) light.y, (int) light.z)
                );
        int skyLight =
                minecraft.level.getBrightness(
                        LightLayer.SKY,
                        new BlockPos((int) light.x, (int) light.y, (int) light.z)
                );
        boolean dark =
                !hasNearbySkyLight(minecraft.level, new BlockPos((int) light.x, (int) light.y, (int) light.z), (int) LIGHT.radius) &&
                !isSkyExposed(minecraft.level, new BlockPos((int) light.x, (int) light.y, (int) light.z), (int) LIGHT.radius) &&
                blockLight <= 4 &&
                        skyLight <= 4;

        Logger.info(blockLight + " " + skyLight + " " + dark);

        shader.getUniform("LightMultiplier").set(dark ? 5 : environmentMultiplier);

        shader.getUniform(
                "ScreenSize"
        ).set(
                (float) minecraft.getWindow().getWidth(),
                (float) minecraft.getWindow().getHeight()
        );

        /*
         * Depth texture.
         */
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(DepthCopy.getTexture());
        shader.setSampler(
                "DepthSampler",
                DepthCopy.getTexture()
        );

        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        RenderSystem.bindTexture(SceneCopy.getTexture());
        shader.setSampler(
                "SceneSampler",
                SceneCopy.getTexture()
        );

        /*
         * Light volume.
         */
        renderSphere(
                poseStack,
                LIGHT.getRadius()
        );

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void renderSphere(
            PoseStack poseStack,
            float radius
    ) {
        Tesselator tesselator =
                Tesselator.getInstance();

        BufferBuilder buffer =
                tesselator.getBuilder();

        buffer.begin(
                VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION
        );

        final int segments = 32;
        final int rings = 16;

        for (int ring = 0; ring < rings; ring++) {

            double phi0 =
                    Math.PI * ring / rings;

            double phi1 =
                    Math.PI * (ring + 1) / rings;

            double y0 = Math.cos(phi0);
            double y1 = Math.cos(phi1);

            double r0 = Math.sin(phi0);
            double r1 = Math.sin(phi1);

            for (int segment = 0; segment < segments; segment++) {

                double theta0 =
                        2.0 * Math.PI * segment / segments;

                double theta1 =
                        2.0 * Math.PI *
                                (segment + 1) /
                                segments;

                float x00 =
                        (float) (Math.cos(theta0) * r0);

                float z00 =
                        (float) (Math.sin(theta0) * r0);

                float x10 =
                        (float) (Math.cos(theta1) * r0);

                float z10 =
                        (float) (Math.sin(theta1) * r0);

                float x01 =
                        (float) (Math.cos(theta0) * r1);

                float z01 =
                        (float) (Math.sin(theta0) * r1);

                float x11 =
                        (float) (Math.cos(theta1) * r1);

                float z11 =
                        (float) (Math.sin(theta1) * r1);

                vertex(buffer, poseStack,
                        x00 * radius,
                        (float) y0 * radius,
                        z00 * radius);

                vertex(buffer, poseStack,
                        x01 * radius,
                        (float) y1 * radius,
                        z01 * radius);

                vertex(buffer, poseStack,
                        x11 * radius,
                        (float) y1 * radius,
                        z11 * radius);

                vertex(buffer, poseStack,
                        x00 * radius,
                        (float) y0 * radius,
                        z00 * radius);

                vertex(buffer, poseStack,
                        x11 * radius,
                        (float) y1 * radius,
                        z11 * radius);

                vertex(buffer, poseStack,
                        x10 * radius,
                        (float) y0 * radius,
                        z10 * radius);
            }
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void vertex(
            BufferBuilder buffer,
            PoseStack poseStack,
            float x,
            float y,
            float z
    ) {
        buffer.vertex(
                poseStack.last().pose(),
                x,
                y,
                z
        ).endVertex();
    }

    public static PointLightTest getLight() {
        return LIGHT;
    }
}
