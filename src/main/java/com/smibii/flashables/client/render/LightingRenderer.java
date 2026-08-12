package com.smibii.flashables.client.render;

import com.smibii.flashables.client.light.LightRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Single entry point for the dynamic lighting pass. Owns the frame's
 * depth snapshot and light-state ticking, then hands off to the point
 * and spot renderers in a fixed order so their scene-copy choreography
 * (each light must see every light drawn before it) stays correct.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightingRenderer {
    private LightingRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        LightRegistry.tick(minecraft.level.getGameTime() / 20.0);

        DepthCopy.copy();

        PointLightRenderer.renderAll(event.getPoseStack(), event.getPartialTick());
        SpotLightRenderer.renderAll(event.getPoseStack(), event.getPartialTick());
    }
}