#ifndef DYNAMIC_LIGHTING_LIGHTS_GLSL
#define DYNAMIC_LIGHTING_LIGHTS_GLSL

struct Light {
    vec3 color;

    float intensity;
    float radius;
};

float calculateAttenuation(
        float distance,
        float radius
) {
    if (distance >= radius) {
        return 0.0;
    }

    float normalized =
    distance / radius;

    return 1.0 -
    normalized * normalized;
}

#endif