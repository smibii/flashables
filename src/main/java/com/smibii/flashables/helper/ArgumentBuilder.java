package com.smibii.flashables.helper;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.Set;

public class ArgumentBuilder<T> extends CommandBuilderItem {
    private final ArgumentType<T> type;
    private Set<String> suggestionSet = null;

    public ArgumentBuilder(String name, ArgumentType<T> type) {
        super(name);
        this.type = type;
    }

    public void suggest(Set<String> set) {
        suggestionSet = set;
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, T> build() {
        RequiredArgumentBuilder<CommandSourceStack, T> builder = RequiredArgumentBuilder.argument(name, type);

        if (suggestionSet != null) {
            builder.suggests((ctx, sbuilder) -> {
                CommandUtils.generateSuggestions(sbuilder, suggestionSet);
                return sbuilder.buildFuture();
            });
        }

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