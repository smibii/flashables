package com.smibii.flashables.client.render.mesh;

import java.util.ArrayList;
import java.util.List;

public class SpotLightMesh extends LightMesh {
    private static final int SEGMENTS = 48;

    public SpotLightMesh() {
        super();

        createMesh(
                createCone()
        );
    }

    private static float[] createCone() {

        List<Float> vertices =
                new ArrayList<>();

        /*
         * Cone:
         *
         * apex = (0, 0, 0)
         *
         * base = z = 1
         *
         * radius = 1
         *
         * The renderer scales the cone so that:
         *
         * z = light radius
         * base radius = tan(angle) * radius
         */

        for (
                int i = 0;
                i < SEGMENTS;
                i++
        ) {

            float a0 =
                    (float) (
                            i *
                                    Math.PI * 2.0 /
                                    SEGMENTS
                    );

            float a1 =
                    (float) (
                            (i + 1) *
                                    Math.PI * 2.0 /
                                    SEGMENTS
                    );

            float x0 =
                    (float) Math.cos(a0);

            float y0 =
                    (float) Math.sin(a0);

            float x1 =
                    (float) Math.cos(a1);

            float y1 =
                    (float) Math.sin(a1);

            /*
             * Side.
             */
            add(
                    vertices,

                    0, 0, 0,

                    x0, y0, 1,

                    x1, y1, 1
            );

            /*
             * Base.
             */
            add(
                    vertices,

                    0, 0, 1,

                    x1, y1, 1,

                    x0, y0, 1
            );
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
