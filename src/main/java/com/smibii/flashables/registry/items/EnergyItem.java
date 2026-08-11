package com.smibii.flashables.registry.items;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class EnergyItem extends Item {
    public EnergyItem(Properties prop) {
        super(prop);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return EnergyNBT.getPower(stack) < EnergyNBT.getMax(stack);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        int power = EnergyNBT.getPower(stack);
        int max = Math.max(1, EnergyNBT.getMax(stack));
        return Math.round(13.0f * power / max);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        float pct = EnergyNBT.getPower(stack) / (float) Math.max(1, EnergyNBT.getMax(stack));
        return Mth.hsvToRgb(pct * 0.33f, 1.0f, 1.0f);
    }
}
