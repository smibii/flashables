package com.smibii.flashables.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.smibii.flashables.Flashables;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SpotLightShader {
    public static ShaderInstance SPOT_LIGHT;

    private SpotLightShader() {}

    @SubscribeEvent
    public static void registerShaders(
            RegisterShadersEvent event
    ) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        Flashables.location("spot_light"),
                        DefaultVertexFormat.POSITION
                ),
                shader -> {
                    SPOT_LIGHT = shader;
                }
        );
    }
}