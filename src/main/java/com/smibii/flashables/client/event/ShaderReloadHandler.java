package com.smibii.flashables.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.smibii.flashables.client.render.PointLightShader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber
public class ShaderReloadHandler {
    public static final KeyMapping RELOAD_SHADERS =
            new KeyMapping(
                    "key.flashables.reload_shaders",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F6,
                    "key.categories.flashables"
            );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RELOAD_SHADERS);
    }

    @Mod.EventBusSubscriber(
            modid = "flashables",
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class InputHandler {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS)
                return;

            if (!RELOAD_SHADERS.consumeClick())
                return;

            reload();
        }
    }

    private static void reload() {
        Minecraft minecraft = Minecraft.getInstance();

        minecraft.reloadResourcePacks();
    }
}