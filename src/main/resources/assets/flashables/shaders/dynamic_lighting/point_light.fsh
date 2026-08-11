#version 150

uniform sampler2D DepthSampler;

uniform vec3 LightPosition;
uniform vec3 LightColor;

uniform float LightIntensity;
uniform float LightRadius;

uniform vec2 ScreenSize;

uniform mat4 InvProjMat;

out vec4 fragColor;

vec3 reconstructViewPosition(
        vec2 uv,
        float depth
) {
    vec4 clipPosition =
    vec4(
            uv * 2.0 - 1.0,
            depth * 2.0 - 1.0,
            1.0
    );

    vec4 viewPosition =
    InvProjMat *
    clipPosition;

    return viewPosition.xyz /
    viewPosition.w;
}

void main() {

    vec2 uv =
    gl_FragCoord.xy /
    ScreenSize;

    float depth =
    texture(
            DepthSampler,
            uv
    ).r;

    if (depth >= 1.0) {
        discard;
    }

    vec3 position =
    reconstructViewPosition(
            uv,
            depth
    );

    vec3 toLight =
    LightPosition -
    position;

    float distance =
    length(toLight);

    if (distance >= LightRadius) {
        discard;
    }

    float attenuation =
    1.0 -
    smoothstep(
            0.0,
            LightRadius,
            distance
    );

    float intensity =
    attenuation *
    LightIntensity;

    fragColor =
    vec4(
            LightColor * intensity,
            1.0
    );
}