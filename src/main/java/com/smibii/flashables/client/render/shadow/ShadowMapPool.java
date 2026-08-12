package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.smibii.flashables.light.PointLight;
import com.smibii.flashables.light.SpotLight;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one {@link PointLightShadowMap}/{@link SpotLightShadowMap} per
 * light instance, since every shadow-casting light needs its own GPU
 * depth texture: they're all rendered once per frame, up front, so
 * unlike the old single-shared-texture design there's no single light
 * "currently being drawn" to reuse a texture for.
 * <p>
 * Neither {@link PointLight} nor {@link SpotLight} override
 * {@code equals}/{@code hashCode}, so keying by the light instance
 * itself is already identity-based.
 */
public final class ShadowMapPool {
    private static final Map<PointLight, PointLightShadowMap> POINT = new ConcurrentHashMap<>();
    private static final Map<SpotLight, SpotLightShadowMap> SPOT = new ConcurrentHashMap<>();

    private ShadowMapPool() {}

    public static PointLightShadowMap forPoint(PointLight light) {
        return POINT.computeIfAbsent(light, l -> {
            PointLightShadowMap map = new PointLightShadowMap();
            map.init();
            return map;
        });
    }

    public static SpotLightShadowMap forSpot(SpotLight light) {
        return SPOT.computeIfAbsent(light, l -> {
            SpotLightShadowMap map = new SpotLightShadowMap();
            map.init();
            return map;
        });
    }

    public static void release(PointLight light) {
        PointLightShadowMap map = POINT.remove(light);
        destroy(map == null ? null : map::destroy);
    }

    public static void release(SpotLight light) {
        SpotLightShadowMap map = SPOT.remove(light);
        destroy(map == null ? null : map::destroy);
    }

    public static void clear() {
        POINT.values().forEach(map -> destroy(map::destroy));
        SPOT.values().forEach(map -> destroy(map::destroy));
        POINT.clear();
        SPOT.clear();
    }

    private static void destroy(Runnable destroyCall) {
        if (destroyCall == null) {
            return;
        }

        if (RenderSystem.isOnRenderThread()) {
            destroyCall.run();
        } else {
            RenderSystem.recordRenderCall(destroyCall::run);
        }
    }
}