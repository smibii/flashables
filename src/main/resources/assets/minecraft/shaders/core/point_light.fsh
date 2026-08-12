#version 150

uniform sampler2D DepthSampler;
uniform sampler2D SceneSampler;
uniform samplerCube ShadowSampler;

uniform mat4 ModelViewMat;
uniform mat4 InvProjMat;
uniform mat4 ProjMat;
uniform mat4 InvViewMat;

uniform vec3 LightPositionView;
uniform vec3 LightPositionWorld;
uniform vec3 LightColor;

uniform float LightIntensity;
uniform float LightRadius;
uniform float LightMultiplier;
uniform float ShadowBias;

uniform vec3 CameraPositionWorld;

uniform vec2 ScreenSize;

out vec4 fragColor;

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

float linearizeShadowDepth(float depth)
{
    float nearPlane = 0.05;
    float farPlane = LightRadius;
    float z = depth * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

float shadowDepthToDistance(vec3 direction, float depth)
{
    float forwardComponent = max(max(abs(direction.x), abs(direction.y)), abs(direction.z));
    forwardComponent = max(forwardComponent, 0.0001);
    float viewDepth = linearizeShadowDepth(depth);
    return viewDepth / forwardComponent;
}

float calculateShadow(vec3 surfaceWorldPosition)
{
    vec3 toSurface = surfaceWorldPosition - LightPositionWorld;
    float currentDistance = length(toSurface);

    if (currentDistance <= 0.001)
    {
        return 1.0;
    }

    vec3 direction = normalize(toSurface);
    float shadowDepth = texture(ShadowSampler, direction).r;

    if (shadowDepth >= 0.999999)
    {
        return 1.0;
    }

    float closestDistance = shadowDepthToDistance(direction, shadowDepth);

    if (closestDistance < currentDistance - ShadowBias)
    {
        return 0.0;
    }

    return 1.0;
}

float calculateSoftShadow(vec3 surfaceWorldPosition)
{
    vec3 toSurface = surfaceWorldPosition - LightPositionWorld;
    float currentDistance = length(toSurface);

    if (currentDistance <= 0.001)
    {
        return 1.0;
    }

    vec3 direction = normalize(toSurface);

    vec3 up = abs(direction.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent = normalize(cross(up, direction));
    vec3 bitangent = normalize(cross(direction, tangent));

    float kernel = 0.0025 + 0.0075 * (currentDistance / LightRadius);
    float visible = 0.0;

    const int SAMPLES = 9;
    vec2 offsets[SAMPLES];

    offsets[0] = vec2(0.0, 0.0);
    offsets[1] = vec2(1.0, 0.0);
    offsets[2] = vec2(-1.0, 0.0);
    offsets[3] = vec2(0.0, 1.0);
    offsets[4] = vec2(0.0, -1.0);
    offsets[5] = vec2(0.707, 0.707);
    offsets[6] = vec2(-0.707, 0.707);
    offsets[7] = vec2(0.707, -0.707);
    offsets[8] = vec2(-0.707, -0.707);

    for (int i = 0; i < SAMPLES; i++)
    {
        vec3 sampleDirection =  normalize(
                                direction +
                                tangent *
                                offsets[i].x *
                                kernel +
                                bitangent *
                                offsets[i].y *
                                kernel);
        float shadowDepth = texture(ShadowSampler, sampleDirection).r;

        if (shadowDepth >= 0.999999)
        {
            visible += 1.0;
            continue;
        }

        float closestDistance = shadowDepthToDistance(sampleDirection, shadowDepth);

        if (closestDistance >= currentDistance - ShadowBias)
        {
            visible += 1.0;
        }
    }

    return visible / float(SAMPLES);
}


void main()
{
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float depth = texture(DepthSampler, uv).r;

    if (depth >= 0.999999)
    {
        discard;
    }

    vec3 surfacePosition = reconstructViewPosition(uv, depth);
    vec3 normal = reconstructNormal(uv, surfacePosition);
    vec3 lightDirection = normalize(LightPositionView - surfacePosition);
    float NdotL = max(dot(normal, lightDirection), 0.0);
    float distanceToLight = distance(surfacePosition, LightPositionView);

    if (distanceToLight >= LightRadius)
    {
        discard;
    }

    float attenuation = 1.0 - distanceToLight / LightRadius;

    attenuation *= attenuation;

    vec4 worldPosition = InvViewMat * vec4(surfacePosition, 1.0);
    vec3 surfaceWorldPosition = worldPosition.xyz + CameraPositionWorld;
    float shadow = calculateSoftShadow(surfaceWorldPosition);
    float amount = NdotL * attenuation * LightIntensity * LightMultiplier * shadow;

    vec3 baseColor = texture(SceneSampler, uv).rgb;
    vec3 lightColor = LightColor * amount;
    vec3 contribution = step(vec3(0.5), baseColor) *
                        (vec3(1.0) - 2.0 *
                        (vec3(1.0) - baseColor) *
                        (vec3(1.0) - lightColor)) +
                        (vec3(1.0) - step(vec3(0.5), baseColor)) *
                        (2.0 * baseColor * lightColor);

    contribution += baseColor;
    contribution = min(contribution, vec3(1.0));

    fragColor = vec4(contribution, 1.0);
}