package com.smibii.flashables.light.types;

import com.smibii.flashables.light.animation.LightState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class PointLight {
    protected Vec3 position = Vec3.ZERO;

    protected Vector3f color =
            new Vector3f(1.0f, 1.0f, 1.0f);

    protected float intensity = 1.0f;

    protected float radius = 10.0f;

    protected boolean enabled = true;

    private final Map<String, LightState> states =
            new HashMap<>();

    private String currentState;
    private String targetState;

    private float stateTransitionTime;
    private float stateTransitionProgress = 1.0f;

    private float previousIntensity;
    private float previousRadius;

    private Vector3f previousColor =
            new Vector3f(1, 1, 1);

    public PointLight() {
        previousIntensity = intensity;
        previousRadius = radius;
        previousColor.set(color);
    }

    public PointLight position(Vec3 position) {
        this.position = position;
        return this;
    }

    public PointLight position(
            double x,
            double y,
            double z
    ) {
        this.position =
                new Vec3(x, y, z);

        return this;
    }

    public PointLight color(
            float r,
            float g,
            float b
    ) {
        this.color =
                new Vector3f(r, g, b);

        return this;
    }

    public PointLight intensity(
            float intensity
    ) {
        this.intensity = intensity;
        return this;
    }

    public PointLight radius(
            float radius
    ) {
        this.radius = radius;
        return this;
    }

    public PointLight enabled(
            boolean enabled
    ) {
        this.enabled = enabled;
        return this;
    }

    public Vec3 getPosition() {
        return position;
    }

    public Vector3f getColor() {
        return color;
    }

    public float getIntensity() {
        return intensity;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PointLight state(
            String name,
            LightState state
    ) {

        states.put(name, state);

        if (currentState == null) {
            currentState = name;
            applyStateInstant(state);
        }

        return this;
    }

    public PointLight state(String name) {

        LightState state =
                states.get(name);

        if (state == null) {
            throw new IllegalArgumentException(
                    "Unknown light state: " + name
            );
        }

        targetState = name;

        previousIntensity =
                intensity;

        previousRadius =
                radius;

        previousColor =
                new Vector3f(
                        color.x(),
                        color.y(),
                        color.z()
                );

        stateTransitionTime =
                state.transitionTime();

        stateTransitionProgress =
                0.0f;

        if (stateTransitionTime <= 0.0f) {

            applyStateInstant(state);

            currentState =
                    name;

            targetState =
                    null;
        }

        return this;
    }

    public void tick(
            float deltaSeconds
    ) {

        if (targetState == null) {
            return;
        }

        LightState target =
                states.get(targetState);

        stateTransitionProgress +=
                deltaSeconds /
                        stateTransitionTime;

        if (
                stateTransitionProgress >= 1.0f
        ) {

            stateTransitionProgress =
                    1.0f;

            applyStateInstant(target);

            currentState =
                    targetState;

            targetState =
                    null;

            return;
        }

        float t =
                target.easing()
                        .apply(
                                stateTransitionProgress
                        );

        intensity =
                lerp(
                        previousIntensity,
                        target.intensity(),
                        t
                );

        radius =
                lerp(
                        previousRadius,
                        target.radius(),
                        t
                );

        color =
                lerpColor(
                        previousColor,
                        target.color(),
                        t
                );
    }

    private void applyStateInstant(
            LightState state
    ) {

        intensity =
                state.intensity();

        radius =
                state.radius();

        color =
                new Vector3f(
                        state.color().x(),
                        state.color().y(),
                        state.color().z()
                );
    }

    private static float lerp(
            float a,
            float b,
            float t
    ) {
        return a + (b - a) * t;
    }

    private static Vector3f lerpColor(
            Vector3f a,
            Vector3f b,
            float t
    ) {

        return new Vector3f(
                lerp(a.x(), b.x(), t),
                lerp(a.y(), b.y(), t),
                lerp(a.z(), b.z(), t)
        );
    }
}
