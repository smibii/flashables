package com.smibii.flashables.client.render.shadow;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PointLightShadowRenderer {
    private static boolean rendering = false;

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (rendering) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        rendering = true;

        try {
//            PointLightShadowMap.render(
//                    event.getPartialTick()
//            );
        } finally {
            rendering = false;
        }
    }
}
