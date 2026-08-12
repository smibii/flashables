#version 150

uniform sampler2D DepthSampler;
uniform sampler2D SceneSampler;

uniform mat4 InvProjMat;
uniform mat4 ProjMat;

uniform vec3 LightPositionView;
uniform vec3 LightColor;

uniform float LightIntensity;
uniform float LightRadius;
uniform float LightMultiplier;

uniform vec2 ScreenSize;

out vec4 fragColor;


/*
 * Reconstruct a view-space position from the depth buffer.
 */
vec3 reconstructViewPosition(vec2 uv, float depth)
{
    float ndcDepth = depth * 2.0 - 1.0;

    vec4 clipPosition = vec4(
            uv * 2.0 - 1.0,
            ndcDepth,
            1.0
    );

    vec4 viewPosition =
    InvProjMat * clipPosition;

    viewPosition /= viewPosition.w;

    return viewPosition.xyz;
}


/*
 * Project a view-space position back into screen UV coordinates.
 */
vec2 projectToScreen(vec3 position)
{
    vec4 clipPosition =
    ProjMat * vec4(position, 1.0);

    clipPosition /= clipPosition.w;

    return clipPosition.xy * 0.5 + 0.5;
}


/*
 * Reconstruct the surface normal from the depth buffer.
 */
vec3 reconstructNormal(
        vec2 uv,
        vec3 position
)
{
    vec2 pixel =
    1.0 / ScreenSize;

    vec3 right =
    reconstructViewPosition(
            uv + vec2(pixel.x, 0.0),
            texture(
                    DepthSampler,
                    uv + vec2(pixel.x, 0.0)
            ).r
    );

    vec3 up =
    reconstructViewPosition(
            uv + vec2(0.0, pixel.y),
            texture(
                    DepthSampler,
                    uv + vec2(0.0, pixel.y)
            ).r
    );

    vec3 tangentX =
    right - position;

    vec3 tangentY =
    up - position;

    return normalize(
            cross(
                    tangentX,
                    tangentY
            )
    );
}


/*
 * Calculate whether the surface can see the light.
 *
 * Returns:
 *
 * 1.0 = fully lit
 * 0.0 = completely shadowed
 *
 * This is a screen-space shadow test.
 *
 * It marches from the surface toward the light
 * and checks the depth buffer for geometry
 * blocking the light.
 */
float calculateShadow(
        vec2 uv,
        vec3 surfacePosition
)
{
    vec3 toLight =
    LightPositionView - surfacePosition;

    float lightDistance =
    length(toLight);

    vec3 direction =
    normalize(toLight);

    /*
     * Number of samples along the shadow ray.
     *
     * Increasing this improves shadow accuracy
     * but increases GPU cost.
     */
    const int STEPS = 32;

    /*
     * Prevent the ray from immediately colliding
     * with the surface it started on.
     */
    const float BIAS = 0.05;

    float stepDistance =
    (lightDistance - BIAS) /
    float(STEPS);

    for (int i = 1; i < STEPS; i++)
    {
        float distance =
        BIAS +
        float(i) * stepDistance;

        vec3 samplePosition =
        surfacePosition +
        direction * distance;

        /*
         * Project the point on the shadow ray
         * into screen space.
         */
        vec2 sampleUV =
        projectToScreen(samplePosition);

        /*
         * If the ray leaves the screen, the depth
         * buffer can no longer tell us whether it
         * intersects geometry.
         */
        if (
            sampleUV.x < 0.0 ||
            sampleUV.x > 1.0 ||
            sampleUV.y < 0.0 ||
            sampleUV.y > 1.0
        )
        {
            continue;
        }

        /*
         * Depth of the visible geometry at this
         * screen position.
         */
        float sceneDepth =
        texture(
                DepthSampler,
                sampleUV
        ).r;

        /*
         * Ignore sky.
         */
        if (sceneDepth >= 0.999999)
        {
            continue;
        }

        /*
         * Reconstruct the visible geometry position.
         */
        vec3 scenePosition =
        reconstructViewPosition(
                sampleUV,
                sceneDepth
        );

        /*
         * Both positions are in view space.
         *
         * Minecraft's view-space Z points toward the
         * camera, so compare the negative Z values.
         */
        float rayDepth =
        -samplePosition.z;

        float scenePositionDepth =
        -scenePosition.z;

        /*
         * Geometry closer to the camera than the ray
         * means that something is blocking the light.
         */
        if (
            scenePositionDepth <
            rayDepth - BIAS
        )
        {
            return 0.0;
        }
    }

    return 1.0;
}


void main()
{
    vec2 uv =
    gl_FragCoord.xy / ScreenSize;

    /*
     * Read the scene depth.
     */
    float depth =
    texture(
            DepthSampler,
            uv
    ).r;

    /*
     * Don't illuminate the sky.
     */
    if (depth >= 0.999999)
    {
        discard;
    }

    /*
     * Reconstruct the visible surface position.
     */
    vec3 surfacePosition =
    reconstructViewPosition(
            uv,
            depth
    );

    /*
     * Reconstruct the surface normal.
     */
    vec3 normal =
    reconstructNormal(
            uv,
            surfacePosition
    );

    /*
     * Direction from the surface toward the light.
     */
    vec3 lightDirection =
    normalize(
            LightPositionView -
            surfacePosition
    );

    /*
     * Lambertian lighting.
     */
    float NdotL =
    max(
            dot(
                    normal,
                    lightDirection
            ),
            0.0
    );

    /*
     * Distance from the surface to the light.
     */
    float distanceToLight =
    distance(
            surfacePosition,
            LightPositionView
    );

    /*
     * Outside the light radius.
     */
    if (distanceToLight >= LightRadius)
    {
        discard;
    }

    /*
     * Smooth distance attenuation.
     */
    float attenuation =
    1.0 -
    distanceToLight /
    LightRadius;

    attenuation *= attenuation;

    /*
     * Shadow visibility.
     */
    float shadow =
    calculateShadow(
            uv,
            surfacePosition
    );

    /*
     * Final light strength.
     */
    float amount =
    NdotL *
    attenuation *
    LightIntensity *
    LightMultiplier *
    shadow;

    /*
     * Existing Minecraft scene.
     */
    vec3 baseColor =
    texture(
            SceneSampler,
            uv
    ).rgb;

    /*
     * Colored light contribution.
     */
    vec3 lightColor =
    LightColor * amount;

    /*
     * Overlay the light onto the existing scene.
     */
    vec3 contribution = step(vec3(0.5), baseColor) * (vec3(1.0) - 2.0 * (vec3(1.0) - baseColor) * (vec3(1.0) - lightColor)) + (vec3(1.0) -
                        step(vec3(0.5), baseColor )) * (2.0 * baseColor * lightColor);

    /*
     * Add the original scene back.
     */
    contribution += baseColor;

    /*
     * Prevent values above the valid color range.
     */
    contribution =
    min(
            contribution,
            vec3(1.0)
    );

    fragColor =
    vec4(
            contribution,
            1.0
    );
}