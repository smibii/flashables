package com.smibii.flashables.client.render;

import com.smibii.flashables.light.types.PointLight;
import com.smibii.flashables.light.types.SpotLight;
import net.minecraft.world.phys.Vec3;

public record GPULight(

        float x,
        float y,
        float z,

        float r,
        float g,
        float b,

        float intensity,
        float radius,

        float dx,
        float dy,
        float dz,

        float innerCos,
        float outerCos,

        float textureId,
        float type,

        float padding

) {
    public static GPULight from(PointLight light) {

        Vec3 position =
                light.getPosition();

        var color =
                light.getColor();

        if (light instanceof SpotLight spot) {

            Vec3 direction =
                    spot.getDirection();

            return new GPULight(

                    (float) position.x,
                    (float) position.y,
                    (float) position.z,

                    light.getRadius(),

                    color.x(),
                    color.y(),
                    color.z(),

                    light.getIntensity(),

                    (float) direction.x,
                    (float) direction.y,
                    (float) direction.z,

                    1.0f,

                    spot.getInnerCos(),
                    spot.getOuterCos(),

                    0.0f,

                    0.0f
            );
        }

        return new GPULight(

                (float) position.x,
                (float) position.y,
                (float) position.z,

                light.getRadius(),

                color.x(),
                color.y(),
                color.z(),

                light.getIntensity(),

                0.0f,
                0.0f,
                0.0f,

                0.0f,

                0.0f,
                0.0f,

                0.0f,

                0.0f
        );
    }
}