package com.smibii.flashables.client.render;

import com.mojang.blaze3d.vertex.*;

/**
 * Shared bounding geometry used to decide which screen pixels a
 * light's fragment shader runs on. A sphere of the light's radius is
 * drawn centered on the camera (world geometry in this render stage
 * is already camera-relative, so local space IS camera space) - since
 * the camera always sits at the center of the sphere, it always
 * covers the whole screen, and the real position/falloff/shadow test
 * happens per-pixel inside the fragment shader using the light's true
 * position. Both point and spot lights reuse this same coverage
 * volume; the fragment shader is what tells them apart.
 */
public final class LightVolumeMesh {
    private LightVolumeMesh() {}

    public static void renderSphere(PoseStack poseStack, float radius) {
        Tesselator tesselator = Tesselator.getInstance();

        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(
                VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION
        );

        final int segments = 32;
        final int rings = 16;

        for (int ring = 0; ring < rings; ring++) {

            double phi0 = Math.PI * ring / rings;
            double phi1 = Math.PI * (ring + 1) / rings;

            double y0 = Math.cos(phi0);
            double y1 = Math.cos(phi1);

            double r0 = Math.sin(phi0);
            double r1 = Math.sin(phi1);

            for (int segment = 0; segment < segments; segment++) {

                double theta0 = 2.0 * Math.PI * segment / segments;
                double theta1 = 2.0 * Math.PI * (segment + 1) / segments;

                float x00 = (float) (Math.cos(theta0) * r0);
                float z00 = (float) (Math.sin(theta0) * r0);

                float x10 = (float) (Math.cos(theta1) * r0);
                float z10 = (float) (Math.sin(theta1) * r0);

                float x01 = (float) (Math.cos(theta0) * r1);
                float z01 = (float) (Math.sin(theta0) * r1);

                float x11 = (float) (Math.cos(theta1) * r1);
                float z11 = (float) (Math.sin(theta1) * r1);

                vertex(buffer, poseStack, x00 * radius, (float) y0 * radius, z00 * radius);
                vertex(buffer, poseStack, x01 * radius, (float) y1 * radius, z01 * radius);
                vertex(buffer, poseStack, x11 * radius, (float) y1 * radius, z11 * radius);

                vertex(buffer, poseStack, x00 * radius, (float) y0 * radius, z00 * radius);
                vertex(buffer, poseStack, x11 * radius, (float) y1 * radius, z11 * radius);
                vertex(buffer, poseStack, x10 * radius, (float) y0 * radius, z10 * radius);
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
        buffer.vertex(poseStack.last().pose(), x, y, z).endVertex();
    }
}