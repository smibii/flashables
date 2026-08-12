package com.smibii.flashables.light;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Light<T extends Light<T>> {
    private Vec3 position = Vec3.ZERO;
    private Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
    private float intensity = 1.0f;
    private float radius = 10.0f;
    private boolean renderShadows = true;

    private final Map<String, LightState> STATES = new HashMap<>();
    private String state;

    private final Map<String, Consumer<Float>> properties = new HashMap<>();

    protected Light() {
        registerProperties();
    }

    protected void registerProperties() {
        property("intensity", this::intensity);

        property("radius", this::radius);

        property("color.r", (value) ->
                color.x = value
        );

        property("color.g", (value) ->
                color.y = value
        );

        property("color.b", (value) ->
                color.z = value
        );

        property("shadows", (value) ->
                renderShadows(value != 0.0)
        );
    }

    protected void property(
            String name,
            Consumer<Float> setter
    ) {
        properties.put(name, setter);
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    public T position(Vec3 position) {
        this.position = position;
        return self();
    }

    public T color(float r, float g, float b) {
        this.color = new Vector3f(r, g, b);
        return self();
    }

    public T color(Vector3f color) {
        this.color = color;
        return self();
    }

    public T intensity(float intensity) {
        this.intensity = intensity;
        return self();
    }

    public T radius(float radius) {
        this.radius = radius;
        return self();
    }

    public T renderShadows(boolean renderShadows) {
        this.renderShadows = renderShadows;
        return self();
    }

    public T state(String name, LightState state) {
        STATES.put(name, state);
        return self();
    }

    public T state(String name) {
        if (STATES.containsKey(name)) {
            state = name;
        }

        return self();
    }

    public LightState getState() {
        return STATES.get(state);
    }

    public void tick(double time) {
        LightState state = getState();

        if (state == null)
            return;

        state.tick(this, time);
    }

    void setProperty(String name, float value) {
        Consumer<Float> setter = properties.get(name);

        if (setter != null) {
            setter.accept(value);
        }
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

    public boolean isRenderShadows() {
        return renderShadows;
    }
}
