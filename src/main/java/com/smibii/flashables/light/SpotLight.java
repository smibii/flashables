package com.smibii.flashables.light;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class SpotLight extends Light<SpotLight> {
    private Vec3 direction = new Vec3(0.0, -1.0, 0.0);
    private float angle = 45.0f;
    private ResourceLocation texture;

    @Override
    protected void registerProperties() {
        super.registerProperties();

        property("angle", this::angle);

        property("direction.x", (value) ->
                direction(value, direction.y, direction.z)
        );

        property("direction.y", (value) ->
                direction(direction.x, value, direction.z)
        );

        property("direction.z", (value) ->
                direction(direction.x, direction.y, value)
        );
    }

    public SpotLight direction(double x, double y, double z) {
        this.direction = new Vec3(x, y, z);
        return this;
    }

    public SpotLight direction(Vec3 direction) {
        this.direction = direction;
        return this;
    }

    public SpotLight angle(float angle) {
        this.angle = angle;
        return this;
    }

    public SpotLight texture(ResourceLocation texture) {
        this.texture = texture;
        return this;
    }

    public Vec3 getDirection() {
        return direction;
    }

    public float getAngle() {
        return angle;
    }

    public ResourceLocation getTexture() {
        return texture;
    }
}
