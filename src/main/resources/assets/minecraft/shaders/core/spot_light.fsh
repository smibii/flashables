#version 150

uniform sampler2D DepthSampler;
uniform sampler2D SceneSampler;
uniform sampler2D ShadowSampler;
uniform sampler2D ProjectedTexture;

uniform mat4 ModelViewMat;
uniform mat4 InvProjMat;
uniform mat4 ProjMat;
uniform mat4 InvViewMat;
uniform mat4 LightShadowMat;

uniform vec3 LightPositionView;
uniform vec3 LightPositionWorld;
uniform vec3 LightDirectionView;
uniform vec3 LightColor;

uniform float LightIntensity;
uniform float LightRadius;
uniform float LightMultiplier;
uniform float ShadowBias;
uniform float LightHasShadows;
uniform float LightVolumetric;
uniform float LightAngleOuterCos;
uniform float LightAngleInnerCos;
uniform float HasProjectedTexture;

uniform vec3 CameraPositionWorld;

uniform vec2 ScreenSize;

out vec4 fragColor;

const int VOLUMETRIC_STEPS = 16;
const float VOLUMETRIC_STRENGTH = 0.035;

vec3 reconstructViewPosition(vec2 uv, float depth)
{
    float ndcDepth = depth * 2.0 - 1.0;
    vec4 clipPosition = vec4(uv * 2.0 - 1.0, ndcDepth, 1.0);
    vec4 viewPosition = InvProjMat * clipPosition;
    viewPosition /= viewPosition.w;
    return viewPosition.xyz;
}

vec3 reconstructNormal(vec2 uv, vec3 position)
{
    vec2 pixel = 1.0 / ScreenSize;
    vec3 right = reconstructViewPosition( uv + vec2(pixel.x, 0.0), texture(DepthSampler, uv + vec2(pixel.x, 0.0)).r);
    vec3 up = reconstructViewPosition(uv + vec2(0.0, pixel.y), texture(DepthSampler, uv + vec2(0.0, pixel.y)).r);
    vec3 tangentX = right - position;
    vec3 tangentY = up - position;
    return normalize(cross(tangentX, tangentY));
}

/*
 * Projects a world-space sample (relative to the light) into the
 * spot light's shadow/cookie frustum. Returns the UV in xy and
 * whether the sample lands inside the frustum in z (1.0 = inside).
 */
vec3 projectToLightSpace(vec3 worldPosition)
{
    vec3 relative = worldPosition - LightPositionWorld;
    vec4 clip = LightShadowMat * vec4(relative, 1.0);

    if (clip.w <= 0.0)
    {
        return vec3(-1.0);
    }

    clip.xyz /= clip.w;

    vec2 uv = 1.0 - (clip.xy * 0.5 + 0.5);;
    float inside = (uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0) ? 1.0 : -1.0;

    return vec3(uv, inside * (clip.z * 0.5 + 0.5 + 1.0));
}

float calculateShadow(vec3 worldPosition)
{
    if (LightHasShadows < 0.5)
    {
        return 1.0;
    }

    vec3 projected = projectToLightSpace(worldPosition);

    if (projected.z < 0.0)
    {
        return 1.0;
    }

    float currentDepth = projected.z - 1.0;
    float storedDepth = texture(ShadowSampler, projected.xy).r;

    if (currentDepth - ShadowBias > storedDepth)
    {
        return 0.0;
    }

    return 1.0;
}

float calculateSoftShadow(vec3 worldPosition)
{
    if (LightHasShadows < 0.5)
    {
        return 1.0;
    }

    vec2 texel = 1.0 / vec2(textureSize(ShadowSampler, 0));
    float visible = 0.0;

    const int SAMPLES = 9;
    vec2 offsets[SAMPLES];

    offsets[0] = vec2(0.0, 0.0);
    offsets[1] = vec2(1.0, 0.0);
    offsets[2] = vec2(-1.0, 0.0);
    offsets[3] = vec2(0.0, 1.0);
    offsets[4] = vec2(0.0, -1.0);
    offsets[5] = vec2(1.0, 1.0);
    offsets[6] = vec2(-1.0, 1.0);
    offsets[7] = vec2(1.0, -1.0);
    offsets[8] = vec2(-1.0, -1.0);

    vec3 projected = projectToLightSpace(worldPosition);

    if (projected.z < 0.0)
    {
        return 1.0;
    }

    float currentDepth = projected.z - 1.0;

    for (int i = 0; i < SAMPLES; i++)
    {
        vec2 sampleUv = projected.xy + offsets[i] * texel * 1.5;
        float storedDepth = texture(ShadowSampler, sampleUv).r;

        if (currentDepth - ShadowBias <= storedDepth)
        {
            visible += 1.0;
        }
    }

    return visible / float(SAMPLES);
}

float spotFalloff(vec3 toSurfaceView)
{
    float cosAngle = dot(toSurfaceView, normalize(LightDirectionView));
    return smoothstep(LightAngleOuterCos, LightAngleInnerCos, cosAngle);
}

vec3 sampleCookie(vec3 worldPosition)
{
    if (HasProjectedTexture < 0.5)
    {
        return vec3(1.0);
    }

    vec3 projected = projectToLightSpace(worldPosition);

    if (projected.z < 0.0)
    {
        return vec3(0.0);
    }

    return texture(ProjectedTexture, projected.xy).rgb;
}

bool intersectSphere(vec3 rayDir, vec3 center, float radius, out float t0, out float t1)
{
    vec3 oc = -center;
    float b = dot(oc, rayDir);
    float c = dot(oc, oc) - radius * radius;
    float h = b * b - c;

    if (h < 0.0)
    {
        return false;
    }

    h = sqrt(h);
    t0 = -b - h;
    t1 = -b + h;
    return true;
}

float hash(vec2 uv)
{
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);
}

float hash3(vec3 p)
{
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise3D(vec3 p)
{
    vec3 i = floor(p);
    vec3 f = fract(p);

    f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

    float n000 = hash3(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));

    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);

    float y0 = mix(x00, x10, f.y);
    float y1 = mix(x01, x11, f.y);

    return mix(y0, y1, f.z);
}

float fbm(vec3 p)
{
    float value = 0.0;
    float amplitude = 0.5;
    value += noise3D(p) * amplitude;
    p = p * 2.0 + vec3(17.0, 31.0, 11.0);
    amplitude *= 0.5;
    value += noise3D(p) * amplitude;
    p = p * 2.0 + vec3(7.0, 19.0, 23.0);
    amplitude *= 0.5;
    value += noise3D(p) * amplitude;
    p = p * 2.0 + vec3(29.0, 13.0, 37.0);
    amplitude *= 0.5;
    value += noise3D(p) * amplitude;
    return value / 0.9375;
}

float fogDensity(vec3 worldPosition)
{
    float large = fbm(worldPosition * 0.025);
    float detail = fbm(worldPosition * 0.075 +vec3(31.7, 17.2, 9.4));
    float density = large * 0.75 + detail * 0.25;
    density = smoothstep(0.42, 0.68, density);
    return density;
}

vec3 calculateVolumetric(
        vec2 uv,
        vec3 rayDir,
        float maxDistance
)
{
    float t0;
    float t1;

    if (!intersectSphere(rayDir, LightPositionView, LightRadius, t0, t1))
    {
        return vec3(0.0);
    }

    t0 = max(t0, 0.0);
    t1 = min(t1, maxDistance);

    if (t1 <= t0)
    {
        return vec3(0.0);
    }

    float stepSize = (t1 - t0) / float(VOLUMETRIC_STEPS);
    float jitter = hash(uv * 0.35);
    vec3 accum = vec3(0.0);
    float transmittance = 1.0;

    for (int i = 0; i < VOLUMETRIC_STEPS; i++)
    {
        float t = t0 + stepSize * (float(i) + jitter * 0.35);
        vec3 samplePosition = rayDir * t;
        vec3 toLight = samplePosition - LightPositionView;
        float distanceToLight = length(toLight);

        if (distanceToLight >= LightRadius)
        {
            continue;
        }

        vec3 lightToSample = normalize(toLight);
        float spot = spotFalloff(lightToSample);

        if (spot <= 0.0)
        {
            continue;
        }

        vec4 worldSample = InvViewMat * vec4(samplePosition, 1.0);
        vec3 worldPosition = worldSample.xyz + CameraPositionWorld;
        float density = fogDensity(worldPosition);

        if (density <= 0.001)
        {
            continue;
        }

        float attenuation = 1.0 - distanceToLight / LightRadius;
        attenuation *= attenuation;
        float shadow = calculateShadow(worldPosition);
        vec3 cookie = sampleCookie(worldPosition);
        float lighting = attenuation * spot * shadow * density;
        vec3 sampleLight = LightColor * cookie * lighting;
        float extinction = density * stepSize * 0.12;
        float sampleTransmittance = exp(-extinction);

        accum += transmittance * sampleLight * stepSize * VOLUMETRIC_STRENGTH * LightIntensity * LightMultiplier;
        transmittance *= sampleTransmittance;

        if (transmittance < 0.01)
        {
            break;
        }
    }

    /*
         * Unclamped, this scales with LightIntensity/LightMultiplier
         * (multiplier alone goes up to 5x - see LightEnvironment) and
         * stepSize (grows with LightRadius), so it can add up to well
         * past "fully bright" before it ever reaches the screen-blend in
         * main(). Screen blend only stays within [0,1] if both inputs
         * already are, so an unbounded value here overshoots even harder
         * than plain addition would - cap it here instead.
         */
    return min(accum, vec3(1.0));
}

void main()
{
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float depth = texture(DepthSampler, uv).r;
    bool hasSurface = depth < 0.999999;

    if (!hasSurface && LightVolumetric < 0.5)
    {
        discard;
    }

    vec3 rayDir = normalize(reconstructViewPosition(uv, 1.0));
    vec3 baseColor = texture(SceneSampler, uv).rgb;
    vec3 contribution = baseColor;

    if (hasSurface)
    {
        vec3 surfacePosition = reconstructViewPosition(uv, depth);
        vec3 normal = reconstructNormal(uv, surfacePosition);
        vec3 lightDirection = normalize(LightPositionView - surfacePosition);
        float NdotL = max(dot(normal, lightDirection), 0.0);
        float distanceToLight = distance(surfacePosition, LightPositionView);
        vec3 toSurface = normalize(surfacePosition - LightPositionView);
        float spot = spotFalloff(toSurface);

        if ((distanceToLight >= LightRadius || spot <= 0.0) && LightVolumetric < 0.5)
        {
            discard;
        }

        if (distanceToLight < LightRadius && spot > 0.0)
        {
            float attenuation = 1.0 - distanceToLight / LightRadius;
            attenuation *= attenuation;

            vec4 worldPosition = InvViewMat * vec4(surfacePosition, 1.0);
            vec3 surfaceWorldPosition = worldPosition.xyz + CameraPositionWorld;
            float shadow = calculateSoftShadow(surfaceWorldPosition);
            vec3 cookie = sampleCookie(surfaceWorldPosition);
            float amount = NdotL * attenuation * spot * LightIntensity * LightMultiplier * shadow;

            vec3 lightColor = LightColor * cookie * amount;
            contribution = step(vec3(0.5), baseColor) *
            (vec3(1.0) - 2.0 *
            (vec3(1.0) - baseColor) *
            (vec3(1.0) - lightColor)) +
            (vec3(1.0) - step(vec3(0.5), baseColor)) *
            (2.0 * baseColor * lightColor);

            contribution += baseColor;
        }
    }

    if (LightVolumetric >= 0.5)
    {
        float maxDistance = hasSurface ? length(reconstructViewPosition(uv, depth)) : LightRadius;
        vec3 volumetric = calculateVolumetric(uv, rayDir, maxDistance);
        /*
         * Same reason as the surface lighting above: plain addition
         * here let the fog stack unbounded on top of an already
         * bright pixel (e.g. looking through this light's volumetric
         * cone at ground already lit directly by a different light),
         * blowing straight past white. Screen-blend it in instead so
         * it tapers the same way.
         */
        contribution = vec3(1.0) - (vec3(1.0) - contribution) * (vec3(1.0) - volumetric);
    }

    contribution = min(contribution, vec3(1.0));

    fragColor = vec4(contribution, 1.0);
}