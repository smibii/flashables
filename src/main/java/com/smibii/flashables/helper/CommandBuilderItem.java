package com.smibii.flashables.helper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandBuilderItem {
    protected final String name;
    protected final List<ArgumentBuilder<?>> arguments = new ArrayList<>();
    protected final List<SubCommandBuilder> subcommands = new ArrayList<>();
    protected Command<CommandSourceStack> command = null;
    protected int permissionLevel = 0;

    public CommandBuilderItem(String name) {
        this.name = name;
    }

    public SubCommandBuilder sub(String name) {
        SubCommandBuilder sub = new SubCommandBuilder(name);
        subcommands.add(sub);
        return sub;
    }

    public <T> ArgumentBuilder<T> argument(String name, ArgumentType<T> type) {
        ArgumentBuilder<T> arg = new ArgumentBuilder<>(name, type);
        arguments.add(arg);
        return arg;
    }

    public void requires(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public void executes(Command<CommandSourceStack> command) {
        this.command = command;
    }

    public abstract <T> T build();
}