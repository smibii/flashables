package com.smibii.flashables.light.config;

import com.smibii.flashables.light.config.builtin.FlashlightDefault;
import org.joml.Vector3f;

public class BuiltInConfigs {
    public static void register() {
        registerFlashlightVariant(0 , "default", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(1 , "white", new Vector3f(1.0f, 1.0f, 1.0f));
        registerFlashlightVariant(2 , "light_gray", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(3 , "gray", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(4 , "black", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(5 , "brown", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(6 , "red", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(7 , "orange", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(8 , "yellow", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(9 , "lime", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(10 , "green", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(11 , "cyan", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(12 , "light_blue", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(13 , "blue", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(14 , "purple", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(15 , "magenta", new Vector3f(1.0f, 0.8f, 0.6f));
        registerFlashlightVariant(16 , "pink", new Vector3f(1.0f, 0.8f, 0.6f));
    }

    private static void registerFlashlightVariant(int index, String colorName, Vector3f color) {
        ConfigManager.register(new FlashlightDefault(true, index, colorName, color));
        ConfigManager.register(new FlashlightDefault(false, index, colorName, color));
    }
}
