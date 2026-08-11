package com.smibii.flashables.registry.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class EnergyNBT {
    public static final String KEY_POWER = "Power";
    public static final String KEY_MAX = "MaxPower";
    public static final int DEFAULT_MAX = 1800;

    public static int getMax(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(KEY_MAX)) tag.putInt(KEY_MAX, DEFAULT_MAX);
        return tag.getInt(KEY_MAX);
    }

    public static int getPower(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(KEY_POWER)) tag.putInt(KEY_POWER, DEFAULT_MAX);
        return tag.getInt(KEY_POWER);
    }

    public static void setPower(ItemStack stack, int value) {
        int max = getMax(stack);
        stack.getOrCreateTag().putInt(KEY_POWER, Math.max(0, Math.min(value, max)));
    }

    public static int insert(ItemStack stack, int amount) {
        int cur = getPower(stack);
        int max = getMax(stack);
        int can = Math.max(0, max - cur);
        int ins = Math.min(can, Math.max(0, amount));
        if (ins > 0) setPower(stack, cur + ins);
        return ins;
    }

    public static int extract(ItemStack stack, int amount) {
        int cur = getPower(stack);
        int ext = Math.min(cur, Math.max(0, amount));
        if (ext > 0) setPower(stack, cur - ext);
        return ext;
    }

    public static boolean exists(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(KEY_POWER);
    }
}
