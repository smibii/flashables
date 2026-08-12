package com.smibii.flashables.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.smibii.flashables.client.render.PointLightRenderer;
import com.smibii.flashables.helper.ArgumentBuilder;
import com.smibii.flashables.helper.CommandBuilder;
import com.smibii.flashables.helper.CommandUtils;
import com.smibii.flashables.helper.SubCommandBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber
public class Light {
    private static final CommandBuilder ROOT = new CommandBuilder("light");

    private static final SubCommandBuilder INTENSITY_SUB = ROOT.sub("intensity");
    private static final ArgumentBuilder<Float> INTENSITY = INTENSITY_SUB.argument("intensity", FloatArgumentType.floatArg());

    private static final SubCommandBuilder COLOR_SUB = ROOT.sub("color");
    private static final ArgumentBuilder<Float> COLOR_R = COLOR_SUB.argument("color_r", FloatArgumentType.floatArg());
    private static final ArgumentBuilder<Float> COLOR_G = COLOR_R.argument("color_g", FloatArgumentType.floatArg());
    private static final ArgumentBuilder<Float> COLOR_B = COLOR_G.argument("color_b", FloatArgumentType.floatArg());

    private static final SubCommandBuilder POSITION_SUB = ROOT.sub("position");
    private static final ArgumentBuilder<Coordinates> POSITION = POSITION_SUB.argument("position", Vec3Argument.vec3());

    private static final SubCommandBuilder RADIUS_SUB = ROOT.sub("radius");
    private static final ArgumentBuilder<Float> RADIUS = RADIUS_SUB.argument("radius", FloatArgumentType.floatArg());

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        INTENSITY.executes(Light::intensity);
        RADIUS.executes(Light::radius);
        COLOR_B.executes(Light::color);
        POSITION.executes(Light::position);

        dispatcher.register(ROOT.build());
    }

    private static int intensity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return CommandUtils.fail(source, "Player is null");

        Float value = context.getArgument("intensity", Float.class);
        PointLightRenderer.LIGHT.intensity = value;

        return CommandUtils.success(source, "Set intensity to " + value);
    }

    private static int radius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return CommandUtils.fail(source, "Player is null");

        Float value = context.getArgument("radius", Float.class);
        PointLightRenderer.LIGHT.radius = value;

        return CommandUtils.success(source, "Set radius to " + value);
    }

    private static int color(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return CommandUtils.fail(source, "Player is null");

        try {
            Float color_r = context.getArgument("color_r", Float.class);
            Float color_g = context.getArgument("color_g", Float.class);
            Float color_b = context.getArgument("color_b", Float.class);
            PointLightRenderer.LIGHT.r = color_r;
            PointLightRenderer.LIGHT.g = color_g;
            PointLightRenderer.LIGHT.b = color_b;

            return CommandUtils.success(source, "Set color to " + color_r + " " + color_g + " " + color_b);
        }
        catch (Exception e) {
            e.printStackTrace();
            return CommandUtils.fail(source, e.getMessage());
        }
    }

    private static int position(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return CommandUtils.fail(source, "Player is null");

        Vec3 pos = context.getArgument("position", Coordinates.class).getPosition(source);
        PointLightRenderer.LIGHT.x = (float) pos.x;
        PointLightRenderer.LIGHT.y = (float) pos.y;
        PointLightRenderer.LIGHT.z = (float) pos.z;

        return CommandUtils.success(source, "Set color to " + pos.x + " " + pos.y + " " + pos.z);
    }
}
