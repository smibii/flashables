package com.smibii.flashables.helper;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public class CommandBuilder extends CommandBuilderItem {
    public CommandBuilder(String name) {
        super(name);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal(name);

        for (SubCommandBuilder sub : subcommands) {
            builder.then(sub.build());
        }

        for (ArgumentBuilder<?> arg : arguments) {
            builder.then(arg.build());
        }

        if (command != null) builder.executes(command);
        builder.requires(source -> source.hasPermission(permissionLevel));

        return builder;
    }
}