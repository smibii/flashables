package com.smibii.flashables.light.animation;

public class Easing {
    private Easing() {}

    public static float linear(float t) {
        return t;
    }

    public static float easeIn(float t) {
        return t * t;
    }

    public static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    public static float easeInOut(float t) {
        return t < 0.5f
                ? 2.0f * t * t
                : 1.0f -
                (float)Math.pow(-2.0f * t + 2.0f, 2) /
                        2.0f;
    }

    public static float smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }
}
