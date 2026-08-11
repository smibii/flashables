package com.smibii.flashables.client;

import com.smibii.flashables.light.LightManager;
import com.smibii.flashables.light.animation.Easing;
import com.smibii.flashables.light.animation.LightState;
import com.smibii.flashables.light.types.SpotLight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class FlashlightTest {
    private static final SpotLight LIGHT =
            new SpotLight();

    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        LIGHT
                .innerAngle(7)
                .outerAngle(18)
                .texture(
                        "dynamiclighting:textures/light/flashlight.png"
                )
                .intensity(1)
                .radius(35)
                .color(
                        1.0f,
                        0.95f,
                        0.85f
                );

        LIGHT.state(
                "OFF",
                LightState.builder()
                        .intensity(0)
                        .radius(0)
                        .transition(0)
                        .build()
        );

        LIGHT.state(
                "ON",
                LightState.builder()
                        .intensity(12)
                        .radius(35)
                        .color(
                                1.0f,
                                0.95f,
                                0.85f
                        )
                        .transition(0.08f)
                        .easing(
                                Easing::easeOut
                        )
                        .build()
        );

        LIGHT.state(
                "DIM",
                LightState.builder()
                        .intensity(4)
                        .radius(20)
                        .color(
                                1.0f,
                                0.8f,
                                0.6f
                        )
                        .transition(0.2f)
                        .build()
        );

        LIGHT.state("ON");
        LIGHT.enabled(true);

        LightManager.add(LIGHT);
    }

    public static void tick() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        Vec3 position =
                minecraft.player.getEyePosition();

        Vec3 direction =
                minecraft.player.getLookAngle();

        LIGHT.position(position);
        LIGHT.direction(direction);
    }

    public static void setState(String state) {
        LIGHT.state(state);
    }

    public static SpotLight getLight() {
        return LIGHT;
    }
}
