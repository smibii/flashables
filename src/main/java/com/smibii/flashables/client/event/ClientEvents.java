package com.smibii.flashables.client.event;

import com.smibii.flashables.Flashables;
import com.smibii.flashables.client.FlashlightTest;
import com.smibii.flashables.client.render.DynamicLightingRenderer;
import com.smibii.flashables.client.render.LightBuffer;
import com.smibii.flashables.light.LightManager;
import com.smibii.flashables.light.types.PointLight;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Flashables.MODID,
        value = Dist.CLIENT
)
public final class ClientEvents {

    @SubscribeEvent
    public static void clientTick(
            TickEvent.ClientTickEvent event
    ) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        FlashlightTest.initialize();
        FlashlightTest.tick();
        LightManager.tick(
                1.0f / 20.0f
        );

        System.out.println(
                "Dynamic lights: " + LightBuffer.getLightCount()
        );
    }

    @SubscribeEvent
    public static void renderLevel(
            RenderLevelStageEvent event
    ) {
        if (
                event.getStage() !=
                        RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
        ) {
            return;
        }

        DynamicLightingRenderer.init();
        DynamicLightingRenderer.prepare();
        DynamicLightingRenderer.render(
                event.getPoseStack(),
                event.getPartialTick()
        );
    }
}
