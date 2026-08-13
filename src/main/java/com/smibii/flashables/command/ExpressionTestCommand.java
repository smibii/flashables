package com.smibii.flashables.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.smibii.flashables.helper.ArgumentBuilder;
import com.smibii.flashables.helper.CommandBuilder;
import com.smibii.flashables.helper.CommandUtils;
import com.smibii.flashables.helper.Logger;
import com.smibii.flashables.light.Expression;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber
public class ExpressionTestCommand {
    private static final CommandBuilder ROOT = new CommandBuilder("exp_test");
    private static final ArgumentBuilder<Double> TIME_ARG = ROOT.argument("time", DoubleArgumentType.doubleArg());
    private static final ArgumentBuilder<Double> STEP_ARG = TIME_ARG.argument("step", DoubleArgumentType.doubleArg());

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        STEP_ARG.executes(ExpressionTestCommand::TEST);

        dispatcher.register(ROOT.build());
    }

    private static int TEST(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return CommandUtils.fail(source, "Player is null");

        Double time = context.getArgument("time", Double.class);
        Double step = context.getArgument("step", Double.class);

        Map<String, Double> values = new HashMap<>();
        values.put("step", step);

        Expression ex = Expression.compile("8 + sin(time * step) * 0.5");
        String content = "8 + sin(" + time + " * " + step + ") * 0.5";
        double result = ex.evaluate(time, values);
        Logger.info("--------------------------------------------------------\n",
                "Content:", content, "\n",
                "Result:", result);

        player.sendSystemMessage(Component.literal(content));
        player.sendSystemMessage(Component.literal(String.valueOf(result)));

        return CommandUtils.success(source, "Success");
    }
}
