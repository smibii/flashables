package com.smibii.flashables.registry.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class Crafting {
    public static final Map<Item, Integer> PANE_TO_COLOR = new HashMap<>();
    public static final Map<Integer, Item> COLOR_TO_PANE = new HashMap<>();
    static {
        PANE_TO_COLOR.put(Items.GLASS_PANE, 0);
        PANE_TO_COLOR.put(Items.WHITE_STAINED_GLASS_PANE, 1);
        PANE_TO_COLOR.put(Items.LIGHT_GRAY_STAINED_GLASS_PANE, 2);
        PANE_TO_COLOR.put(Items.GRAY_STAINED_GLASS_PANE, 3);
        PANE_TO_COLOR.put(Items.BLACK_STAINED_GLASS_PANE, 4);
        PANE_TO_COLOR.put(Items.BROWN_STAINED_GLASS_PANE, 5);
        PANE_TO_COLOR.put(Items.RED_STAINED_GLASS_PANE, 6);
        PANE_TO_COLOR.put(Items.ORANGE_STAINED_GLASS_PANE, 7);
        PANE_TO_COLOR.put(Items.YELLOW_STAINED_GLASS_PANE, 8);
        PANE_TO_COLOR.put(Items.LIME_STAINED_GLASS_PANE, 9);
        PANE_TO_COLOR.put(Items.GREEN_STAINED_GLASS_PANE, 10);
        PANE_TO_COLOR.put(Items.CYAN_STAINED_GLASS_PANE, 11);
        PANE_TO_COLOR.put(Items.LIGHT_BLUE_STAINED_GLASS_PANE, 12);
        PANE_TO_COLOR.put(Items.BLUE_STAINED_GLASS_PANE, 13);
        PANE_TO_COLOR.put(Items.PURPLE_STAINED_GLASS_PANE, 14);
        PANE_TO_COLOR.put(Items.MAGENTA_STAINED_GLASS_PANE, 15);
        PANE_TO_COLOR.put(Items.PINK_STAINED_GLASS_PANE, 16);
        PANE_TO_COLOR.forEach((item, idx) -> COLOR_TO_PANE.put(idx, item));
    }

    public static int getFlashlightColor(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("Color") : 0;
    }
}
