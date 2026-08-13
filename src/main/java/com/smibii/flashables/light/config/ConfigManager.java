package com.smibii.flashables.light.config;

import com.smibii.flashables.Flashables;
import com.smibii.flashables.helper.Logger;
import com.smibii.flashables.light.config.builtin.BuiltInConfigData;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigManager {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(Flashables.MODID + "/lights");
    private static final Map<String, LightConfig> CONFIGS = new HashMap<>();

    public static void init() {
        if (Files.notExists(CONFIG_PATH)) {
            try {
                Files.createDirectories(CONFIG_PATH);
            } catch (IOException e) {
                Logger.error("Error while trying to create config directory.");
            }
        }

        CONFIGS.clear();
        BuiltInConfigs.register();

        for (Path path : GetFilePaths(CONFIG_PATH)) {
            String file = path.getFileName().toString();
            if (!file.endsWith(".json")) continue;

            try {
                LightConfig config = LightConfig.parse(path);
                register(file, config);
                Logger.success("Successfully loaded config:", file);
            }
            catch (Exception e) {
                Logger.error("Error while trying to load config:", file);
                Logger.error(e.getMessage());
            }
        }
    }

    private static List<Path> GetFilePaths(Path basePath)
    {
        try (Stream<Path> stream = Files.list(basePath)) {
            return stream.collect(Collectors.toList());
        } catch (IOException e) {
            Logger.error("Failed to read overlay directory contents", e);
            return List.of();
        }
    }

    public static void register(BuiltInConfigData builtIn) {
        Logger.info("(" + CONFIGS.keySet().toArray().length + ") Registering built-in config:", builtIn.file());
        LightConfig config = new LightConfig(builtIn);
        register(builtIn.file(), config);
    }

    public static void register(String file, LightConfig config) {
        CONFIGS.put(file, config);
    }

    public static void unregister(Path path) {
        CONFIGS.remove(path);
    }

    public static Map<String, LightConfig> configs() {
        return CONFIGS;
    }
}
