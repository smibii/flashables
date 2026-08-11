#version 150

uniform sampler2D DepthSampler;
uniform sampler2D LightTexture;

uniform vec3 LightPosition;
uniform vec3 LightDirection;
uniform vec3 LightColor;

uniform float LightIntensity;
uniform float LightRadius;

uniform float InnerCos;
uniform float OuterCos;

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

vec2 projectTexture(
        vec3 position
) {

    vec3 forward =
    normalize(
            LightDirection
    );

    vec3 up =
    vec3(0.0, 1.0, 0.0);

    if (
        abs(
                dot(
                        forward,
                        up
                )
        ) > 0.99
    ) {
        up =
        vec3(0.0, 0.0, 1.0);
    }

    vec3 right =
    normalize(
            cross(
                    forward,
                    up
            )
    );

    up =
    normalize(
            cross(
                    right,
                    forward
            )
    );

    vec3 relative =
    position -
    LightPosition;

    float depth =
    dot(
            relative,
            forward
    );

    if (depth <= 0.0) {
        return vec2(-1.0);
    }

    float x =
    dot(
            relative,
            right
    ) / depth;

    float y =
    dot(
            relative,
            up
    ) / depth;

    return vec2(
            x * 0.5 + 0.5,
            y * 0.5 + 0.5
    );
}

void main() {

    vec2 screenUV =
    gl_FragCoord.xy /
    ScreenSize;

    float depth =
    texture(
            DepthSampler,
            screenUV
    ).r;

    if (depth >= 1.0) {
        discard;
    }

    vec3 position =
    reconstructViewPosition(
            screenUV,
            depth
    );

    vec3 toLight =
    LightPosition -
    position;

    float distance =
    length(toLight);

    if (
        distance >=
        LightRadius
    ) {
        discard;
    }

    vec3 direction =
    normalize(toLight);

    float cone =
    dot(
            normalize(-direction),
            normalize(LightDirection)
    );

    if (
        cone <= OuterCos
    ) {
        discard;
    }

    float spot =
    smoothstep(
            OuterCos,
            InnerCos,
            cone
    );

    float attenuation =
    1.0 -
    smoothstep(
            0.0,
            LightRadius,
            distance
    );

    vec2 textureUV =
    projectTexture(
            position
    );

    if (
        textureUV.x < 0.0 ||
        textureUV.x > 1.0 ||
        textureUV.y < 0.0 ||
        textureUV.y > 1.0
    ) {
        discard;
    }

    vec4 textureColor =
    texture(
            LightTexture,
            textureUV
    );

    float contribution =
    LightIntensity *
    attenuation *
    spot *
    textureColor.a;

    fragColor =
    vec4(
            LightColor *
            textureColor.rgb *
            contribution,
            1.0
    );
}