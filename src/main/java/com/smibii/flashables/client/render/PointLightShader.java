package com.smibii.flashables.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.smibii.flashables.Flashables;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PointLightShader {
    public static ShaderInstance POINT_LIGHT;

    private PointLightShader() {}

    @SubscribeEvent
    public static void registerShaders(
            RegisterShadersEvent event
    ) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        Flashables.location("point_light"),
                        DefaultVertexFormat.POSITION
                ),
                shader -> {
                    POINT_LIGHT = shader;
                }
        );
    }
}