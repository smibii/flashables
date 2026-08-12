package com.smibii.flashables.client.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class PointLightTest {
    public float x = -5.0f;
    public float y = 97.0f;
    public float z = 20.0f;
    public float r = 1.0f;
    public float g = 1.0f;
    public float b = 1.0f;
    public float intensity = 1.0f;
    public float radius = 10.0f;

    public Vec3 getPosition() {
        return new Vec3(x, y, z);
    }

    public Vector3f getColor() {
        return new Vector3f(r, g, b);
    }

    public float getIntensity() {
        return intensity;
    }

    public float getRadius() {
        return radius;
    }
}