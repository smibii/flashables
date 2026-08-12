package com.smibii.flashables.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.smibii.flashables.client.light.LightRegistry;
import com.smibii.flashables.helper.ArgumentBuilder;
import com.smibii.flashables.helper.CommandBuilder;
import com.smibii.flashables.helper.CommandUtils;
import com.smibii.flashables.helper.SubCommandBuilder;
import com.smibii.flashables.light.PointLight;
import com.smibii.flashables.light.SpotLight;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber
public class LightCommand {
    private static final CommandBuilder ROOT = new CommandBuilder("light");

    private static final SubCommandBuilder SPAWN = ROOT.sub("spawn");

    private static final SubCommandBuilder SPAWN_POINT = SPAWN.sub("point");
    private static final ArgumentBuilder<Float> SPAWN_POINT_RADIUS = SPAWN_POINT.argument("radius", FloatArgumentType.floatArg(0.1f));
    private static final ArgumentBuilder<Float> SPAWN_POINT_INTENSITY = SPAWN_POINT_RADIUS.argument("intensity", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder SPAWN_SPOT = SPAWN.sub("spot");
    private static final ArgumentBuilder<Float> SPAWN_SPOT_RADIUS = SPAWN_SPOT.argument("radius", FloatArgumentType.floatArg(0.1f));
    private static final ArgumentBuilder<Float> SPAWN_SPOT_INTENSITY = SPAWN_SPOT_RADIUS.argument("intensity", FloatArgumentType.floatArg(0.0f));
    private static final ArgumentBuilder<Float> SPAWN_SPOT_ANGLE = SPAWN_SPOT_INTENSITY.argument("angle", FloatArgumentType.floatArg(1.0f, 89.0f));

    private static final SubCommandBuilder LIST = ROOT.sub("list");
    private static final SubCommandBuilder CLEAR = ROOT.sub("clear");

    private static final SubCommandBuilder REMOVE = ROOT.sub("remove");
    private static final SubCommandBuilder REMOVE_POINT = REMOVE.sub("point");
    private static final ArgumentBuilder<Integer> REMOVE_POINT_INDEX = REMOVE_POINT.argument("index", IntegerArgumentType.integer(0));
    private static final SubCommandBuilder REMOVE_SPOT = REMOVE.sub("spot");
    private static final ArgumentBuilder<Integer> REMOVE_SPOT_INDEX = REMOVE_SPOT.argument("index", IntegerArgumentType.integer(0));

    private static final SubCommandBuilder POINT = ROOT.sub("point");
    private static final ArgumentBuilder<Integer> POINT_INDEX = POINT.argument("index", IntegerArgumentType.integer(0));

    private static final SubCommandBuilder POINT_RADIUS = POINT_INDEX.sub("radius");
    private static final ArgumentBuilder<Float> POINT_RADIUS_VALUE = POINT_RADIUS.argument("value", FloatArgumentType.floatArg(0.1f));

    private static final SubCommandBuilder POINT_INTENSITY = POINT_INDEX.sub("intensity");
    private static final ArgumentBuilder<Float> POINT_INTENSITY_VALUE = POINT_INTENSITY.argument("value", FloatArgumentType.floatArg(0.0f));

    private static final SubCommandBuilder POINT_COLOR = POINT_INDEX.sub("color");
    private static final ArgumentBuilder<Float> POINT_COLOR_R = POINT_COLOR.argument("r", FloatArgumentType.floatArg(0.0f, 1.0f));
    private static final ArgumentBuilder<Float> POINT_COLOR_G = POINT_COLOR_R.argument("g", FloatArgumentType.floatArg(0.0f, 1.0f));
    private static final ArgumentBuilder<Float> POINT_COLOR_B = POINT_COLOR_G.argument("b", FloatArgumentType.floatArg(0.0f, 1.0f));

    private static final SubCommandBuilder POINT_SHADOWS = POINT_INDEX.sub("shadows");
    private static final ArgumentBuilder<Boolean> POINT_SHADOWS_VALUE = POINT_SHADOWS.argument("value", BoolArgumentType.bool());

    private static final SubCommandBuilder POINT_VOLUMETRIC = POINT_INDEX.sub("volumetric");
    private static final ArgumentBuilder<Boolean> POINT_VOLUMETRIC_VALUE = POINT_VOLUMETRIC.argument("value", BoolArgumentType.bool());

    private static final SubCommandBuilder POINT_MOVETO = POINT_INDEX.sub("moveto");

    private static final SubCommandBuilder SPOT = ROOT.sub("spot");
    private static final ArgumentBuilder<Integer> SPOT_INDEX = SPOT.argument("index", IntegerArgumentType.integer(0));

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

    private static final SubCommandBuilder SPOT_ANGLE = SPOT_INDEX.sub("angle");
    private static final ArgumentBuilder<Float> SPOT_ANGLE_VALUE = SPOT_ANGLE.argument("value", FloatArgumentType.floatArg(1.0f, 89.0f));

    private static final SubCommandBuilder SPOT_LOOK = SPOT_INDEX.sub("look");

    private static final SubCommandBuilder SPOT_TEXTURE = SPOT_INDEX.sub("texture");
    private static final ArgumentBuilder<ResourceLocation> SPOT_TEXTURE_VALUE = SPOT_TEXTURE.argument("value", ResourceLocationArgument.id());

    private static final SubCommandBuilder SPOT_NOTEXTURE = SPOT_INDEX.sub("notexture");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        ROOT.requires(2);

        SPAWN_POINT.executes(LightCommand::spawnPoint);
        SPAWN_POINT_RADIUS.executes(LightCommand::spawnPoint);
        SPAWN_POINT_INTENSITY.executes(LightCommand::spawnPoint);

        SPAWN_SPOT.executes(LightCommand::spawnSpot);
        SPAWN_SPOT_RADIUS.executes(LightCommand::spawnSpot);
        SPAWN_SPOT_INTENSITY.executes(LightCommand::spawnSpot);
        SPAWN_SPOT_ANGLE.executes(LightCommand::spawnSpot);

        LIST.executes(LightCommand::list);
        CLEAR.executes(LightCommand::clear);

        REMOVE_POINT_INDEX.executes(LightCommand::removePoint);
        REMOVE_SPOT_INDEX.executes(LightCommand::removeSpot);

        POINT_RADIUS_VALUE.executes(LightCommand::pointRadius);
        POINT_INTENSITY_VALUE.executes(LightCommand::pointIntensity);
        POINT_COLOR_B.executes(LightCommand::pointColor);
        POINT_SHADOWS_VALUE.executes(LightCommand::pointShadows);
        POINT_VOLUMETRIC_VALUE.executes(LightCommand::pointVolumetric);
        POINT_MOVETO.executes(LightCommand::pointMoveTo);

        SPOT_RADIUS_VALUE.executes(LightCommand::spotRadius);
        SPOT_INTENSITY_VALUE.executes(LightCommand::spotIntensity);
        SPOT_COLOR_B.executes(LightCommand::spotColor);
        SPOT_SHADOWS_VALUE.executes(LightCommand::spotShadows);
        SPOT_VOLUMETRIC_VALUE.executes(LightCommand::spotVolumetric);
        SPOT_ANGLE_VALUE.executes(LightCommand::spotAngle);
        SPOT_LOOK.executes(LightCommand::spotLook);
        SPOT_TEXTURE_VALUE.executes(LightCommand::spotTexture);
        SPOT_NOTEXTURE.executes(LightCommand::spotNoTexture);

        dispatcher.register(ROOT.build());
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

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        int points = LightRegistry.getPointLights().size();
        int spots = LightRegistry.getSpotLights().size();

        return CommandUtils.success(source, points + " point light(s), " + spots + " spot light(s)");
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        LightRegistry.clear();
        return CommandUtils.success(source, "Cleared all lights");
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

        light.radius(FloatArgumentType.getFloat(context, "value"));
        return CommandUtils.success(source, "Updated radius");
    }

    private static int pointIntensity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        light.intensity(FloatArgumentType.getFloat(context, "value"));
        return CommandUtils.success(source, "Updated intensity");
    }

    private static int pointColor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        light.color(new Vector3f(
                FloatArgumentType.getFloat(context, "r"),
                FloatArgumentType.getFloat(context, "g"),
                FloatArgumentType.getFloat(context, "b")
        ));
        return CommandUtils.success(source, "Updated color");
    }

    private static int pointShadows(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        light.renderShadows(BoolArgumentType.getBool(context, "value"));
        return CommandUtils.success(source, "Updated shadows");
    }

    private static int pointVolumetric(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        light.renderVolumetric(BoolArgumentType.getBool(context, "value"));
        return CommandUtils.success(source, "Updated volumetric lighting");
    }

    private static int pointMoveTo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        if (player == null) return CommandUtils.fail(source, "Player is null");

        PointLight light = resolvePoint(context);
        if (light == null) return CommandUtils.fail(source, "No such point light");

        light.position(player.getEyePosition());
        return CommandUtils.success(source, "Moved point light");
    }

    private static int spotRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.radius(FloatArgumentType.getFloat(context, "value"));
        return CommandUtils.success(source, "Updated radius");
    }

    private static int spotIntensity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.intensity(FloatArgumentType.getFloat(context, "value"));
        return CommandUtils.success(source, "Updated intensity");
    }

    private static int spotColor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.color(new Vector3f(
                FloatArgumentType.getFloat(context, "r"),
                FloatArgumentType.getFloat(context, "g"),
                FloatArgumentType.getFloat(context, "b")
        ));
        return CommandUtils.success(source, "Updated color");
    }

    private static int spotShadows(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.renderShadows(BoolArgumentType.getBool(context, "value"));
        return CommandUtils.success(source, "Updated shadows");
    }

    private static int spotVolumetric(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.renderVolumetric(BoolArgumentType.getBool(context, "value"));
        return CommandUtils.success(source, "Updated volumetric lighting");
    }

    private static int spotAngle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.angle(FloatArgumentType.getFloat(context, "value"));
        return CommandUtils.success(source, "Updated angle");
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
        light.texture(texture);
        return CommandUtils.success(source, "Updated projected texture");
    }

    private static int spotNoTexture(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpotLight light = resolveSpot(context);
        if (light == null) return CommandUtils.fail(source, "No such spot light");

        light.texture(null);
        return CommandUtils.success(source, "Removed projected texture");
    }
}
