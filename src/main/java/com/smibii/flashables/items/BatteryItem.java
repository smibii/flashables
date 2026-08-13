package com.smibii.flashables.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BatteryItem extends EnergyItem {
    public BatteryItem(Properties prop) {
        super(prop);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Power: " + EnergyNBT.getPower(stack) + " / " + EnergyNBT.getMax(stack)));
    }
}
