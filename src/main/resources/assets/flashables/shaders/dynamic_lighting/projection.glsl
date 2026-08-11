#ifndef DYNAMIC_LIGHTING_PROJECTION_GLSL
#define DYNAMIC_LIGHTING_PROJECTION_GLSL

vec2 projectTexture(
        vec3 worldPosition,
        vec3 lightPosition,
        vec3 lightDirection
) {
    vec3 forward =
    normalize(lightDirection);

    vec3 upReference =
    vec3(0.0, 1.0, 0.0);

    if (abs(dot(forward, upReference)) > 0.99) {
        upReference =
        vec3(0.0, 0.0, 1.0);
    }

    vec3 right =
    normalize(
            cross(
                    forward,
                    upReference
            )
    );

    vec3 up =
    normalize(
            cross(
                    right,
                    forward
            )
    );

    vec3 relative =
    worldPosition -
    lightPosition;

    float depth =
    dot(relative, forward);

    if (depth <= 0.0) {
        return vec2(-1.0);
    }

    float x =
    dot(relative, right) / depth;

    float y =
    dot(relative, up) / depth;

    return vec2(
            x * 0.5 + 0.5,
            y * 0.5 + 0.5
    );
}

#endif