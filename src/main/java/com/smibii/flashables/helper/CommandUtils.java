package com.smibii.flashables.helper;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandUtils {
    public static int success(CommandSourceStack source, Component msg) {
        source.sendSuccess(() -> msg, false);
        return 1;
    }

    public static int success(CommandSourceStack source, String msg) {
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    public static int fail(CommandSourceStack source, Component msg) {
        source.sendFailure(msg);
        return 0;
    }

    public static int fail(CommandSourceStack source, String msg) {
        source.sendFailure(Component.literal(msg));
        return 0;
    }

    public static Component colorizeJson(String title, String json) {
        MutableComponent formattedJson = MutableComponent.create(ComponentContents.EMPTY);
        formattedJson.append(Component.literal(title + ": ").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));

        Pattern pattern = Pattern.compile("(\\[|\\]|\\{|\\}|, |,|: |\"[^\"]*\")|(true)|(false)|(-?\\d+(\\.\\d+)?)|(null)");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                String match = matcher.group(1);

                if ("[]{}, : ".contains(match)) {
                    formattedJson.append(Component.literal(match).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));
                }

                else if (match.startsWith("\"")) {
                    formattedJson.append(Component.literal(match).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)));
                }
            }

            else if (matcher.group(2) != null) {
                String match = matcher.group(2);
                if ("true".equals(match)) {
                    formattedJson.append(Component.literal(match).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
                }
            }

            else if (matcher.group(3) != null) {
                String match = matcher.group(3);
                if ("false".equals(match)) {
                    formattedJson.append(Component.literal(match).setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
            }

            else if (matcher.group(4) != null) {
                String match = matcher.group(4);
                formattedJson.append(Component.literal(match).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
            }

            else if (matcher.group(5) != null) {
                String match = matcher.group(5);
                formattedJson.append(Component.literal(match).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)));
            }
        }

        return formattedJson;
    }

    public static void generateSuggestions(SuggestionsBuilder builder, Set<String> list) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        list.stream()
                .filter(name -> name.startsWith(remaining))
                .map(String::toLowerCase)
                .sorted()
                .forEach(builder::suggest);
    }

    public static String fetchArgument(CommandContext<CommandSourceStack> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }
}