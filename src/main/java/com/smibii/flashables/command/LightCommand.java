package com.smibii.flashables.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.smibii.flashables.client.light.LightRegistry;
import com.smibii.flashables.helper.*;
import com.smibii.flashables.light.LightState;
import com.smibii.flashables.light.PointLight;
import com.smibii.flashables.light.SpotLight;
import com.smibii.flashables.light.config.BuiltInConfigs;
import com.smibii.flashables.light.config.ConfigManager;
import com.smibii.flashables.light.config.LightConfig;
import com.smibii.flashables.light.config.builtin.BuiltInConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.*;

@Mod.EventBusSubscriber
public class LightCommand {
    private static final CommandBuilder ROOT = new CommandBuilder("light");

    private static final SubCommandBuilder LIST = ROOT.sub("list");
    private static final SubCommandBuilder CLEAR = ROOT.sub("clear");

    private static final SubCommandBuilder CONFIG = ROOT.sub("config");
    private static final SubCommandBuilder CONFIG_RELOAD = CONFIG.sub("reload");

    private static final SubCommandBuilder CONFIG_REGISTRY = CONFIG.sub("registry");
    private static final SubCommandBuilder CONFIG_REGISTRY_LIST = CONFIG_REGISTRY.sub("list");

    private static final SubCommandBuilder CONFIG_REGISTRY_GET = CONFIG_REGISTRY.sub("get");
    private static final ArgumentBuilder<String> CONFIG_REGISTRY_GET_FILE = CONFIG_REGISTRY_GET.argument("file", StringArgumentType.string());
    private static final ArgumentBuilder<String> CONFIG_REGISTRY_GET_FILE_PROPERTY = CONFIG_REGISTRY_GET_FILE.argument("property", StringArgumentType.string());

    private static final SubCommandBuilder POINT = ROOT.sub("point");
    private static final SubCommandBuilder ADD_POINT = POINT.sub("add");
    private static final ArgumentBuilder<Float> ADD_POINT_RADIUS = ADD_POINT.argument("radius", FloatArgumentType.floatArg(0.1f));
    private static final ArgumentBuilder<Float> ADD_POINT_INTENSITY = ADD_POINT_RADIUS.argument("intensity", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder REMOVE_POINT = POINT.sub("remove");
    private static final ArgumentBuilder<Integer> REMOVE_POINT_INDEX = REMOVE_POINT.argument("index", IntegerArgumentType.integer(0));

    private static final SubCommandBuilder EDIT_POINT = POINT.sub("edit");
    private static final ArgumentBuilder<Integer> EDIT_POINT_INDEX = EDIT_POINT.argument("index", IntegerArgumentType.integer(0));

    private static final SubCommandBuilder POINT_RADIUS = EDIT_POINT_INDEX.sub("radius");
    private static final ArgumentBuilder<Float> POINT_RADIUS_VALUE = POINT_RADIUS.argument("value", FloatArgumentType.floatArg(0.1f));

    private static final SubCommandBuilder POINT_INTENSITY = EDIT_POINT_INDEX.sub("intensity");
    private static final ArgumentBuilder<Float> POINT_INTENSITY_VALUE = POINT_INTENSITY.argument("value", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder POINT_COLOR = EDIT_POINT_INDEX.sub("color");
    private static final ArgumentBuilder<Float> POINT_COLOR_R = POINT_COLOR.argument("r", FloatArgumentType.floatArg(0.0f, 1.0f));
    private static final ArgumentBuilder<Float> POINT_COLOR_G = POINT_COLOR_R.argument("g", FloatArgumentType.floatArg(0.0f, 1.0f));
    private static final ArgumentBuilder<Float> POINT_COLOR_B = POINT_COLOR_G.argument("b", FloatArgumentType.floatArg(0.0f, 1.0f));

    private static final SubCommandBuilder POINT_SHADOWS = EDIT_POINT_INDEX.sub("shadows");
    private static final ArgumentBuilder<Boolean> POINT_SHADOWS_VALUE = POINT_SHADOWS.argument("value", BoolArgumentType.bool());

    private static final SubCommandBuilder POINT_VOLUMETRIC = EDIT_POINT_INDEX.sub("volumetric");
    private static final ArgumentBuilder<Boolean> POINT_VOLUMETRIC_VALUE = POINT_VOLUMETRIC.argument("value", BoolArgumentType.bool());

    private static final SubCommandBuilder POINT_VOLUMETRIC_STRENGTH = EDIT_POINT_INDEX.sub("volumetric_strength");
    private static final ArgumentBuilder<Float> POINT_VOLUMETRIC_STRENGTH_VALUE =POINT_VOLUMETRIC_STRENGTH.argument("value", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder POINT_VOLUMETRIC_STEP = EDIT_POINT_INDEX.sub("volumetric_step");
    private static final ArgumentBuilder<Float> POINT_VOLUMETRIC_STEP_VALUE = POINT_VOLUMETRIC_STEP.argument("value", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder POINT_MOVETO = EDIT_POINT_INDEX.sub("moveto");

    private static final SubCommandBuilder POINT_STATE = EDIT_POINT_INDEX.sub("state");

    private static final SubCommandBuilder POINT_STATE_ADD = POINT_STATE.sub("add");
    private static final ArgumentBuilder<String> POINT_STATE_ADD_NAME = POINT_STATE_ADD.argument("name", StringArgumentType.string());

    private static final SubCommandBuilder POINT_STATE_SET = POINT_STATE.sub("set");
    private static final ArgumentBuilder<String> POINT_STATE_SET_NAME = POINT_STATE_SET.argument("name", StringArgumentType.string());

    private static final SubCommandBuilder SPOT = ROOT.sub("spot");

    private static final SubCommandBuilder ADD_SPOT = SPOT.sub("add");
    private static final ArgumentBuilder<Float> ADD_SPOT_RADIUS = ADD_SPOT.argument("radius", FloatArgumentType.floatArg(0.1f));
    private static final ArgumentBuilder<Float> ADD_SPOT_INTENSITY = ADD_SPOT_RADIUS.argument("intensity", FloatArgumentType.floatArg(0.0f));
    private static final ArgumentBuilder<Float> ADD_SPOT_ANGLE = ADD_SPOT_INTENSITY.argument("angle", FloatArgumentType.floatArg(1.0f, 89.0f));

    private static final SubCommandBuilder REMOVE_SPOT = SPOT.sub("remove");
    private static final ArgumentBuilder<Integer> REMOVE_SPOT_INDEX = REMOVE_SPOT.argument("index", IntegerArgumentType.integer(0));

    private static final SubCommandBuilder EDIT_SPOT = SPOT.sub("edit");
    private static final ArgumentBuilder<Integer> SPOT_INDEX = EDIT_SPOT.argument("index", IntegerArgumentType.integer(0));

    private static final SubCommandBuilder SPOT_RADIUS = SPOT_INDEX.sub("radius");
    private static final ArgumentBuilder<Float> SPOT_RADIUS_VALUE = SPOT_RADIUS.argument("value", FloatArgumentType.floatArg(0.1f));

    private static final SubCommandBuilder SPOT_INTENSITY = SPOT_INDEX.sub("intensity");
    private static final ArgumentBuilder<Float> SPOT_INTENSITY_VALUE = SPOT_INTENSITY.argument("value", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder SPOT_COLOR = SPOT_INDEX.sub("color");
    private static final ArgumentBuilder<Float> SPOT_COLOR_R = SPOT_COLOR.argument("r", FloatArgumentType.floatArg(0.0f, 1.0f));
    private static final ArgumentBuilder<Float> SPOT_COLOR_G = SPOT_COLOR_R.argument("g", FloatArgumentType.floatArg(0.0f, 1.0f));
    private static final ArgumentBuilder<Float> SPOT_COLOR_B = SPOT_COLOR_G.argument("b", FloatArgumentType.floatArg(0.0f, 1.0f));

    private static final SubCommandBuilder SPOT_SHADOWS = SPOT_INDEX.sub("shadows");
    private static final ArgumentBuilder<Boolean> SPOT_SHADOWS_VALUE = SPOT_SHADOWS.argument("value", BoolArgumentType.bool());

    private static final SubCommandBuilder SPOT_VOLUMETRIC = SPOT_INDEX.sub("volumetric");
    private static final ArgumentBuilder<Boolean> SPOT_VOLUMETRIC_VALUE = SPOT_VOLUMETRIC.argument("value", BoolArgumentType.bool());

    private static final SubCommandBuilder SPOT_VOLUMETRIC_STRENGTH = SPOT_INDEX.sub("volumetric_strength");
    private static final ArgumentBuilder<Float> SPOT_VOLUMETRIC_STRENGTH_VALUE = SPOT_VOLUMETRIC_STRENGTH.argument("value", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder SPOT_VOLUMETRIC_STEP = SPOT_INDEX.sub("volumetric_step");
    private static final ArgumentBuilder<Float> SPOT_VOLUMETRIC_STEP_VALUE = SPOT_VOLUMETRIC_STEP.argument("value", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder SPOT_ANGLE = SPOT_INDEX.sub("angle");
    private static final ArgumentBuilder<Float> SPOT_ANGLE_VALUE = SPOT_ANGLE.argument("value", FloatArgumentType.floatArg(1.0f, 89.0f));

    private static final SubCommandBuilder SPOT_LOOK = SPOT_INDEX.sub("look");

    private static final SubCommandBuilder SPOT_TEXTURE = SPOT_INDEX.sub("texture");
    private static final ArgumentBuilder<ResourceLocation> SPOT_TEXTURE_VALUE = SPOT_TEXTURE.argument("value", ResourceLocationArgument.id());

    private static final SubCommandBuilder SPOT_NOTEXTURE = SPOT_INDEX.sub("notexture");

    private static final SubCommandBuilder SPOT_STATE = SPOT_INDEX.sub("state");

    private static final SubCommandBuilder SPOT_STATE_ADD = SPOT_STATE.sub("add");
    private static final ArgumentBuilder<String> SPOT_STATE_ADD_NAME = SPOT_STATE_ADD.argument("name", StringArgumentType.string());

    private static final SubCommandBuilder SPOT_STATE_SET = SPOT_STATE.sub("set");
    private static final ArgumentBuilder<String> SPOT_STATE_SET_NAME = SPOT_STATE_SET.argument("name", StringArgumentType.string());

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        ROOT.requires(2);

        LIST.executes(LightCommand::list);
        CLEAR.executes(LightCommand::clear);

        CONFIG_RELOAD.executes(LightCommand::configReload);
        CONFIG_REGISTRY_LIST.executes(LightCommand::configRegistryList);
        CONFIG_REGISTRY_GET_FILE.suggest(ConfigManager.configs().keySet());
        CONFIG_REGISTRY_GET_FILE_PROPERTY.suggest(BuiltInConfigData.properties());
        CONFIG_REGISTRY_GET_FILE_PROPERTY.executes(LightCommand::configRegistryGetFileProperty);

        ADD_POINT.executes(LightCommand::spawnPoint);
        ADD_POINT_RADIUS.executes(LightCommand::spawnPoint);
        ADD_POINT_INTENSITY.executes(LightCommand::spawnPoint);

        ADD_SPOT.executes(LightCommand::spawnSpot);
        ADD_SPOT_RADIUS.executes(LightCommand::spawnSpot);
        ADD_SPOT_INTENSITY.executes(LightCommand::spawnSpot);
        ADD_SPOT_ANGLE.executes(LightCommand::spawnSpot);

        REMOVE_POINT_INDEX.executes(LightCommand::removePoint);
        REMOVE_SPOT_INDEX.executes(LightCommand::removeSpot);

        POINT_RADIUS_VALUE.executes(LightCommand::pointRadius);
        POINT_INTENSITY_VALUE.executes(LightCommand::pointIntensity);
        POINT_COLOR_B.executes(LightCommand::pointColor);
        POINT_SHADOWS_VALUE.executes(LightCommand::pointShadows);
        POINT_VOLUMETRIC_VALUE.executes(LightCommand::pointVolumetric);
        POINT_VOLUMETRIC_STRENGTH_VALUE.executes(LightCommand::pointVolumetricStrength);
        POINT_VOLUMETRIC_STEP_VALUE.executes(LightCommand::pointVolumetricStep);
        POINT_MOVETO.executes(LightCommand::pointMoveTo);
        POINT_STATE_ADD_NAME.suggest(Set.of("name"));
        registerList(POINT_STATE_ADD_NAME, "point", new PointLight().getPropertyNames());
        POINT_STATE_SET_NAME.executes(LightCommand::pointStateSet);

        SPOT_RADIUS_VALUE.executes(LightCommand::spotRadius);
        SPOT_INTENSITY_VALUE.executes(LightCommand::spotIntensity);
        SPOT_COLOR_B.executes(LightCommand::spotColor);
        SPOT_SHADOWS_VALUE.executes(LightCommand::spotShadows);
        SPOT_VOLUMETRIC_VALUE.executes(LightCommand::spotVolumetric);
        SPOT_VOLUMETRIC_STRENGTH_VALUE.executes(LightCommand::spotVolumetricStrength);
        SPOT_VOLUMETRIC_STEP_VALUE.executes(LightCommand::spotVolumetricStep);
        SPOT_ANGLE_VALUE.executes(LightCommand::spotAngle);
        SPOT_LOOK.executes(LightCommand::spotLook);
        SPOT_TEXTURE_VALUE.executes(LightCommand::spotTexture);
        SPOT_NOTEXTURE.executes(LightCommand::spotNoTexture);
        SPOT_STATE_ADD_NAME.suggest(Set.of("name"));
        registerList(SPOT_STATE_ADD_NAME, "spot", new SpotLight().getPropertyNames());
        SPOT_STATE_SET_NAME.executes(LightCommand::spotStateSet);

        dispatcher.register(ROOT.build());
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        int points = LightRegistry.getPointLights().size();
        int spots = LightRegistry.getSpotLights().size();

        return CommandUtils.success(source, points + " point light(s), " + spots + " spot light(s)");
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        LightRegistry.clear();
        return CommandUtils.success(source, "Cleared all lights!");
    }

    private static int configReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ConfigManager.init();
        return CommandUtils.success(source, "Config reloaded!");
    }

    private static int configRegistryList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Set<String> configs = ConfigManager.configs().keySet();
        return CommandUtils.success(source, configs.size() + " config(s) registered: " + configs);
    }

    private static int configRegistryGetFileProperty(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Map<String, LightConfig> configs = ConfigManager.configs();
        String file = StringArgumentType.getString(context, "file");
        String property = StringArgumentType.getString(context, "property");

        if (!configs.containsKey(file)) return CommandUtils.success(source, "Config file \"" + file + "\" not found!");
        if (!BuiltInConfigData.properties().contains(property)) return CommandUtils.success(source, "Config doesn't have a property called \"" + file + "\"!");

        LightConfig config = configs.get(file);
        Object value = config.property(property);
        return CommandUtils.success(source, "Property \"" + property + "\" has value \"" + value + "\"");
    }

    private static void registerList(ArgumentBuilder<String> sub, String type, Set<String> set) {
        for (int i = 0; i < set.size(); i++) {
            ArgumentBuilder<String> property = sub.argument("property" + i, StringArgumentType.word());
            property.suggest(set);

            ArgumentBuilder<String> value = property.argument("value" + i, StringArgumentType.string());
            if (type == "point") value.executes(LightCommand::pointStateAdd);
            else value.executes(LightCommand::spotStateSet);

            sub = value;
            Logger.info("Permuting " + type + " " + set);
        }
    }

    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> context) {
        return context.getSource().getPlayer();
    }

    private static float optionalFloat(CommandContext<CommandSourceStack> context, String name, float fallback) {
        try {
            return FloatArgumentType.getFloat(context, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static int spawnPoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        float radius = optionalFloat(context, "radius", 10.0f);
        float intensity = optionalFloat(context, "intensity", 1.0f);

        PointLight light = new PointLight()
                .position(player.getEyePosition())
                .radius(radius)
                .intensity(intensity);

        LightRegistry.addPointLight(light);

        int index = LightRegistry.getPointLights().size() - 1;
        return CommandUtils.success(source, "Spawned point light #" + index);
    }

    private static int spawnSpot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        float radius = optionalFloat(context, "radius", 12.0f);
        float intensity = optionalFloat(context, "intensity", 1.0f);
        float angle = optionalFloat(context, "angle", 30.0f);

        SpotLight light = new SpotLight()
                .position(player.getEyePosition())
                .direction(player.getLookAngle())
                .radius(radius)
                .intensity(intensity)
                .angle(angle);

        LightRegistry.addSpotLight(light);

        int index = LightRegistry.getSpotLights().size() - 1;
        return CommandUtils.success(source, "Spawned spot light #" + index);
    }

    private static int removePoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int index = IntegerArgumentType.getInteger(context, "index");
        PointLight removed = LightRegistry.removePointLight(index);

        if (removed == null) return CommandUtils.fail(source, "No point light #" + index);
        return CommandUtils.success(source, "Removed point light #" + index);
    }

    private static int removeSpot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int index = IntegerArgumentType.getInteger(context, "index");
        SpotLight removed = LightRegistry.removeSpotLight(index);

        if (removed == null) return CommandUtils.fail(source, "No spot light #" + index);
        return CommandUtils.success(source, "Removed spot light #" + index);
    }

    private static PointLight resolvePoint(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        return LightRegistry.getPointLight(index);
    }

    private static SpotLight resolveSpot(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        return LightRegistry.getSpotLight(index);
    }

    private static int pointRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.radius(value);
        return CommandUtils.success(source, "Set radius to " + value);
    }

    private static int pointIntensity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.intensity(value);
        return CommandUtils.success(source, "Set intensity to " + value);
    }

    private static int pointColor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        Vector3f value = new Vector3f(
                FloatArgumentType.getFloat(context, "r"),
                FloatArgumentType.getFloat(context, "g"),
                FloatArgumentType.getFloat(context, "b")
        );
        light.color(value);
        return CommandUtils.success(source, "Set color to " + value);
    }

    private static int pointShadows(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        boolean value = BoolArgumentType.getBool(context, "value");
        light.renderShadows(value);
        return CommandUtils.success(source, "Set draw shadows to " + value);
    }

    private static int pointVolumetric(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        boolean value = BoolArgumentType.getBool(context, "value");
        light.renderVolumetric(value);
        return CommandUtils.success(source, "Set volumetric lighting to " + value);
    }

    private static int pointVolumetricStrength(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.volumetricStrength(value);
        return CommandUtils.success(source, "Set volumetric strength to " + value);
    }

    private static int pointVolumetricStep(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.volumetricStep(value);
        return CommandUtils.success(source, "Set volumetric step to " + value);
    }

    private static int pointMoveTo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        light.position(player.getEyePosition());
        return CommandUtils.success(source, "Moved point light to " + player.getEyePosition());
    }

    private static int pointStateAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        String name = StringArgumentType.getString(context, "name");
        LightState state = new LightState();
        for (int i = 0; i < 50; i++) {
            String property = StringArgumentType.getString(context, "property" + i);
            String value = StringArgumentType.getString(context, "value" + i);
            if (property == null || value == null) break;
            state.add(property, value);
        }

        light.state(name, state);
        return CommandUtils.success(source, "Added state " + name + " to light");
    }

    private static int pointStateSet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        String name = StringArgumentType.getString(context, "name");
        light.state(name);
        return CommandUtils.success(source, "Set state to " + name);
    }

    private static int spotRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.radius(value);
        return CommandUtils.success(source, "Set radius to " + value);
    }

    private static int spotIntensity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.intensity(value);
        return CommandUtils.success(source, "Set intensity to " + value);
    }

    private static int spotColor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        Vector3f value = new Vector3f(
                FloatArgumentType.getFloat(context, "r"),
                FloatArgumentType.getFloat(context, "g"),
                FloatArgumentType.getFloat(context, "b")
        );
        light.color(value);
        return CommandUtils.success(source, "Set color to " + value);
    }

    private static int spotShadows(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        boolean value = BoolArgumentType.getBool(context, "value");
        light.renderShadows(value);
        return CommandUtils.success(source, "Set draw shadows to " + value);
    }

    private static int spotVolumetric(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        boolean value = BoolArgumentType.getBool(context, "value");
        light.renderVolumetric(value);
        return CommandUtils.success(source, "Set volumetric lighting to " + value);
    }

    private static int spotVolumetricStrength(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.volumetricStrength(value);
        return CommandUtils.success(source, "Set volumetric strength to " + value);
    }

    private static int spotVolumetricStep(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.volumetricStep(value);
        return CommandUtils.success(source, "Set volumetric step to " + value);
    }


    private static int spotAngle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        float value = FloatArgumentType.getFloat(context, "value");
        light.angle(value);
        return CommandUtils.success(source, "Set angle to " + value);
    }

    private static int spotLook(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.position(player.getEyePosition());
        light.direction(player.getLookAngle());
        return CommandUtils.success(source, "Moved spot light to look direction");
    }

    private static int spotTexture(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        ResourceLocation texture = ResourceLocationArgument.getId(context, "value");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return CommandUtils.fail(source, "No such texture called " + texture);

        light.texture(texture);
        return CommandUtils.success(source, "Set projected texture to " + texture);
    }

    private static int spotNoTexture(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.texture(null);
        return CommandUtils.success(source, "Removed projected texture");
    }

    private static int spotStateAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        String name = StringArgumentType.getString(context, "name");
        LightState state = new LightState();
        for (int i = 0; i < 50; i++) {
            String property = StringArgumentType.getString(context, "property" + i);
            String value = StringArgumentType.getString(context, "value" + i);
            if (property == null || value == null) break;
            state.add(property, value);
        }

        light.state(name, state);
        return CommandUtils.success(source, "Added state " + name);
    }

    private static int spotStateSet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        String name = StringArgumentType.getString(context, "name");
        light.state(name);
        return CommandUtils.success(source, "Set state to " + name);
    }
}
