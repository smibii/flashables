package com.smibii.flashables.light.types;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class SpotLight extends PointLight {
    private Vec3 direction =
            new Vec3(0, 0, 1);

    private float innerAngle = 10.0f;
    private float outerAngle = 20.0f;

    private ResourceLocation texture;

    public SpotLight direction(
            Vec3 direction
    ) {

        this.direction =
                direction.normalize();

        return this;
    }

    public SpotLight direction(
            double x,
            double y,
            double z
    ) {

        this.direction =
                new Vec3(x, y, z)
                        .normalize();

        return this;
    }

    public SpotLight innerAngle(
            float angle
    ) {

        this.innerAngle =
                angle;

        return this;
    }

    public SpotLight outerAngle(
            float angle
    ) {

        this.outerAngle =
                angle;

        return this;
    }

    public SpotLight texture(
            ResourceLocation texture
    ) {

        this.texture =
                texture;

        return this;
    }

    public SpotLight texture(
            String texture
    ) {

        this.texture =
                ResourceLocation.parse(
                        texture
                );

        return this;
    }

    public Vec3 getDirection() {
        return direction;
    }

    public float getInnerAngle() {
        return innerAngle;
    }

    public float getOuterAngle() {
        return outerAngle;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public float getInnerCos() {

        return (float)
                Math.cos(
                        Math.toRadians(
                                innerAngle
                        )
                );
    }

    public float getOuterCos() {

        return (float)
                Math.cos(
                        Math.toRadians(
                                outerAngle
                        )
                );
    }
}
