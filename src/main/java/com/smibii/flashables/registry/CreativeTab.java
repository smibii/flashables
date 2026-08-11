package com.smibii.flashables.registry;

import com.smibii.flashables.Flashables;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Flashables.MODID);

    public static final RegistryObject<CreativeModeTab> FLASHABLES_TAB = TABS.register("flashables",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(FlashablesItems.FLASHLIGHT.get()))
                    .title(Component.translatable("itemGroup." + Flashables.MODID + ".flasables"))
                    .withTabsBefore(new ResourceLocation("minecraft", "ingredients"))
                    .displayItems((params, output) -> {
                        output.accept(new ItemStack(FlashablesItems.BATTERY.get()));
                        for (int color = 0; color < 16; color++) {
                            output.accept(flashlight(color));
                        }
                    })
                    .build());

    private static ItemStack flashlight(int color) {
        ItemStack stack = new ItemStack(FlashablesItems.FLASHLIGHT.get());
        stack.getOrCreateTag().putInt("Color", color);
        return stack;
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
