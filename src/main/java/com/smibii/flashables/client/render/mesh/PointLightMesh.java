package com.smibii.flashables.client.render.mesh;

import java.util.ArrayList;
import java.util.List;

public class PointLightMesh extends LightMesh {
    private static final int SEGMENTS = 32;
    private static final int RINGS = 16;

    public PointLightMesh() {
        super();

        createMesh(
                createSphere()
        );
    }

    private static float[] createSphere() {

        List<Float> vertices =
                new ArrayList<>();

        for (int ring = 0; ring < RINGS; ring++) {

            float v0 =
                    (float) ring / RINGS;

            float v1 =
                    (float) (ring + 1) / RINGS;

            float phi0 =
                    (float) (
                            Math.PI * v0 -
                                    Math.PI / 2.0
                    );

            float phi1 =
                    (float) (
                            Math.PI * v1 -
                                    Math.PI / 2.0
                    );

            float y0 =
                    (float) Math.sin(phi0);

            float y1 =
                    (float) Math.sin(phi1);

            float r0 =
                    (float) Math.cos(phi0);

            float r1 =
                    (float) Math.cos(phi1);

            for (
                    int segment = 0;
                    segment < SEGMENTS;
                    segment++
            ) {

                float u0 =
                        (float) segment /
                                SEGMENTS;

                float u1 =
                        (float) (segment + 1) /
                                SEGMENTS;

                float theta0 =
                        (float) (
                                u0 * Math.PI * 2.0
                        );

                float theta1 =
                        (float) (
                                u1 * Math.PI * 2.0
                        );

                float x00 =
                        r0 * (float) Math.cos(theta0);

                float z00 =
                        r0 * (float) Math.sin(theta0);

                float x01 =
                        r0 * (float) Math.cos(theta1);

                float z01 =
                        r0 * (float) Math.sin(theta1);

                float x10 =
                        r1 * (float) Math.cos(theta0);

                float z10 =
                        r1 * (float) Math.sin(theta0);

                float x11 =
                        r1 * (float) Math.cos(theta1);

                float z11 =
                        r1 * (float) Math.sin(theta1);

                add(
                        vertices,
                        x00, y0, z00,
                        x10, y1, z10,
                        x11, y1, z11
                );

                add(
                        vertices,
                        x00, y0, z00,
                        x11, y1, z11,
                        x01, y0, z01
                );
            }
        }

        return createFloatArray(vertices);
    }

    private static void add(
            List<Float> vertices,
            float... values
    ) {

        for (float value : values) {
            vertices.add(value);
        }
    }
}
