package com.smibii.flashables.client.render.shadow;

import com.smibii.flashables.client.light.LightRegistry;
import com.smibii.flashables.light.PointLight;
import com.smibii.flashables.light.SpotLight;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders every light's shadow map once per frame, before the frame's
 * normal level render begins.
 * <p>
 * This has to happen outside {@code RenderLevelStageEvent}: that
 * event fires from inside an already-in-progress
 * {@code LevelRenderer.renderLevel()} call, and each shadow map is
 * itself rendered by calling {@code renderLevel()} again with a
 * shadow camera. Doing that from inside {@code RenderLevelStageEvent}
 * re-enters {@code renderLevel()} while the outer call is still on
 * the stack, which corrupts Oculus/Iris's per-frame terrain layer
 * state (its {@code endTerrainLayer} bookkeeping isn't reentrant) and
 * crashes the game. {@code RenderTickEvent.Phase.START} fires before
 * {@code GameRenderer.render()}/{@code renderLevel()} are called at
 * all for the frame, so rendering shadow maps here is a separate,
 * non-nested top-level call instead.
 * <p>
 * That still leaves one nesting problem: each shadow map's own
 * {@code renderLevel()} call (one per cubemap face, for point lights)
 * is itself a real level render, so it fires
 * {@code RenderLevelStageEvent} too - which would re-trigger
 * {@link com.smibii.flashables.client.render.LightingRenderer} and draw every light's sphere again, mid
 * shadow-pass, into whatever framebuffer/viewport/projection the
 * shadow map currently has bound. {@link #isActive()} lets
 * {@code LightingRenderer} detect that and skip its own pass while
 * this one is running.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowPassRenderer {
    private static boolean active = false;

    private ShadowPassRenderer() {}

    /**
     * True while this class is in the middle of rendering shadow maps
     * for the frame - including while a shadow map's own nested
     * {@code renderLevel()} call is re-entering the render pipeline.
     */
    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        float partialTick = event.renderTickTime;

        active = true;

        try {
            for (PointLight light : LightRegistry.getPointLights()) {
                if (!light.isRenderShadows()) {
                    continue;
                }

                ShadowMapPool.forPoint(light).render(light.getPosition(), light.getRadius(), partialTick);
            }

            for (SpotLight light : LightRegistry.getSpotLights()) {
                if (!light.isRenderShadows() && light.getTexture() == null) {
                    continue;
                }

                ShadowMapPool.forSpot(light).render(
                        light.getPosition(),
                        light.getDirection(),
                        light.getAngle(),
                        light.getRadius(),
                        partialTick
                );
            }
        } finally {
            active = false;
        }
    }
}