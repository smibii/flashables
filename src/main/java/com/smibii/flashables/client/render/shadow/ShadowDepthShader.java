package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.smibii.flashables.Flashables;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/**
 * Minimal position-only, depth-only shader used by
 * {@link ShadowGeometryRenderer}. Registered by this mod (rather than
 * relying on {@code GameRenderer.getPositionShader()}) so the
 * ModelViewMat/ProjMat uniform names are guaranteed to match what
 * {@link ShadowGeometryRenderer} sets, the same way the point/spot
 * light shaders already are.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShadowDepthShader {
    public static ShaderInstance SHADOW_DEPTH;

    private ShadowDepthShader() {}

    @SubscribeEvent
    public static void registerShaders(
            RegisterShadersEvent event
    ) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        Flashables.location("shadow_depth"),
                        DefaultVertexFormat.POSITION
                ),
                shader -> {
                    SHADOW_DEPTH = shader;
                }
        );
    }
}
