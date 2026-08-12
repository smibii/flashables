package com.smibii.flashables.client.light;

import com.smibii.flashables.client.render.shadow.ShadowMapPool;
import com.smibii.flashables.light.PointLight;
import com.smibii.flashables.light.SpotLight;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side registry of the light sources that are currently
 * rendered in the world. Populated by commands, items, or entities;
 * consumed once per frame by the light renderers.
 */
public final class LightRegistry {
    private static final List<PointLight> POINT_LIGHTS = new CopyOnWriteArrayList<>();
    private static final List<SpotLight> SPOT_LIGHTS = new CopyOnWriteArrayList<>();

    private LightRegistry() {}

    public static PointLight addPointLight(PointLight light) {
        POINT_LIGHTS.add(light);
        return light;
    }

    public static SpotLight addSpotLight(SpotLight light) {
        SPOT_LIGHTS.add(light);
        return light;
    }

    public static boolean removePointLight(PointLight light) {
        boolean removed = POINT_LIGHTS.remove(light);

        if (removed) {
            ShadowMapPool.release(light);
        }

        return removed;
    }

    public static boolean removeSpotLight(SpotLight light) {
        boolean removed = SPOT_LIGHTS.remove(light);

        if (removed) {
            ShadowMapPool.release(light);
        }

        return removed;
    }

    public static List<PointLight> getPointLights() {
        return Collections.unmodifiableList(POINT_LIGHTS);
    }

    public static List<SpotLight> getSpotLights() {
        return Collections.unmodifiableList(SPOT_LIGHTS);
    }

    public static PointLight getPointLight(int index) {
        if (index < 0 || index >= POINT_LIGHTS.size()) {
            return null;
        }

        return POINT_LIGHTS.get(index);
    }

    public static SpotLight getSpotLight(int index) {
        if (index < 0 || index >= SPOT_LIGHTS.size()) {
            return null;
        }

        return SPOT_LIGHTS.get(index);
    }

    public static PointLight removePointLight(int index) {
        PointLight light = getPointLight(index);

        if (light != null) {
            removePointLight(light);
        }

        return light;
    }

    public static SpotLight removeSpotLight(int index) {
        SpotLight light = getSpotLight(index);

        if (light != null) {
            removeSpotLight(light);
        }

        return light;
    }

    public static void clear() {
        POINT_LIGHTS.clear();
        SPOT_LIGHTS.clear();
        ShadowMapPool.clear();
    }

    public static void tick(double time) {
        for (PointLight light : POINT_LIGHTS) {
            light.tick(time);
        }

        for (SpotLight light : SPOT_LIGHTS) {
            light.tick(time);
        }
    }
}