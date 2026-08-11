package com.smibii.flashables.light;

import com.smibii.flashables.light.types.PointLight;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LightManager {
    private static final List<PointLight> LIGHTS = new ArrayList<>();

    private LightManager() {}

    public static void add(PointLight light) {
        if (!LIGHTS.contains(light)) {
            LIGHTS.add(light);
        }
    }

    public static void remove(PointLight light) {
        LIGHTS.remove(light);
    }

    public static void clear() {
        LIGHTS.clear();
    }

    public static List<PointLight> getLights() {
        return Collections.unmodifiableList(LIGHTS);
    }

    public static void tick(float deltaSeconds) {

        for (PointLight light : LIGHTS) {
            light.tick(deltaSeconds);
        }
    }

    public static List<PointLight> getVisibleLights(
            Vec3 cameraPosition
    ) {

        List<PointLight> result =
                new ArrayList<>();

        for (PointLight light : LIGHTS) {

            if (!light.isEnabled()) {
                continue;
            }

            double radius = light.getRadius();

            double distance =
                    light.getPosition()
                            .distanceTo(cameraPosition);

            if (distance <= radius) {
                result.add(light);
            }
        }

        return result;
    }

    public static List<PointLight> getVisibleLights(
            Vec3 cameraPosition,
            int maxLights
    ) {

        List<PointLight> lights =
                getVisibleLights(cameraPosition);

        lights.sort((a, b) -> {

            double da =
                    a.getPosition()
                            .distanceToSqr(cameraPosition);

            double db =
                    b.getPosition()
                            .distanceToSqr(cameraPosition);

            return Double.compare(da, db);
        });

        if (lights.size() > maxLights) {
            return new ArrayList<>(
                    lights.subList(0, maxLights)
            );
        }

        return lights;
    }
}
