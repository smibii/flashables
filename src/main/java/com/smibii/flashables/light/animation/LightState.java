package com.smibii.flashables.light.animation;

import org.joml.Vector3f;

import java.util.function.Function;

public record LightState (
        float intensity,
        float radius,
        Vector3f color,
        float transitionTime,
        Function<Float, Float> easing
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private float intensity = 1.0f;
        private float radius = 10.0f;
        private Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        private float transitionTime = 0;
        private Function<Float, Float> easing = Easing::linear;

        public Builder intensity(float intensity) {
            this.intensity = intensity;
            return this;
        }

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder color(
                float r,
                float g,
                float b
        ) {
            this.color = new Vector3f(r, g, b);
            return this;
        }

        public Builder transition(float seconds) {
            this.transitionTime = seconds;
            return this;
        }

        public Builder easing(Function<Float, Float> easing) {
            this.easing = easing;
            return this;
        }

        public LightState build() {
            return new LightState(
                    intensity,
                    radius,
                    color,
                    transitionTime,
                    easing
            );
        }
    }
}
