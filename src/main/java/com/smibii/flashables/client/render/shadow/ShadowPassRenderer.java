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
 * {@code LevelRenderer.renderLevel()} call. {@link PointLightShadowMap}
 * and {@link SpotLightShadowMap} used to render their depth by calling
 * {@code renderLevel()} again with a shadow camera, which - called
 * from inside {@code RenderLevelStageEvent} - re-entered
 * {@code renderLevel()} while the outer call was still on the stack,
 * corrupting Oculus/Iris's per-frame state and crashing or hanging the
 * game in a different subsystem every time (terrain layers, then
 * weather rendering). They now build and draw shadow geometry
 * themselves (see {@link ShadowGeometryRenderer}) instead of calling
 * back into {@code LevelRenderer} at all, which avoids that whole
 * class of problem rather than working around one symptom of it.
 * {@code RenderTickEvent.Phase.START} still fires before
 * {@code GameRenderer.render()} for the frame, so this remains a
 * separate, non-nested top-level call - no longer strictly required
 * for correctness now that nothing here re-enters the level renderer,
 * but there's no reason to move it.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowPassRenderer {
    private static boolean active = false;

    private ShadowPassRenderer() {}

    /**
     * True while this class is in the middle of rendering shadow maps
     * for the frame. Kept as a defensive guard for
     * {@code LightingRenderer} even though nothing here re-enters
     * {@code RenderLevelStageEvent} anymore.
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

        active = true;

        try {
            for (PointLight light : LightRegistry.getPointLights()) {
                if (!light.isRenderShadows()) {
                    continue;
                }

                ShadowMapPool.forPoint(light).render(light.getPosition(), light.getRadius());
            }

            for (SpotLight light : LightRegistry.getSpotLights()) {
                boolean wantsShadow = light.isRenderShadows();
                boolean wantsCookie = light.getTexture() != null;

                if (!wantsShadow && !wantsCookie) {
                    continue;
                }

                SpotLightShadowMap map = ShadowMapPool.forSpot(light);

                if (wantsShadow) {
                    map.render(light.getPosition(), light.getDirection(), light.getAngle(), light.getRadius());
                } else {
                    /*
                     * Shadows off, but the light still has a
                     * projected texture - that only needs an
                     * up-to-date matrix, not a rendered depth
                     * texture, so skip the (now real, not free) GPU
                     * work of render().
                     */
                    map.updateShadowMatOnly(light.getDirection(), light.getAngle(), light.getRadius());
                }
            }
        } finally {
            active = false;
        }
    }
}
