package com.smibii.flashables.registry;

import com.smibii.flashables.Flashables;
import com.smibii.flashables.registry.items.BatteryItem;
import com.smibii.flashables.registry.items.FlashlightItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FlashablesItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Flashables.MODID);

    public static final RegistryObject<Item> BATTERY = ITEMS.register(
            "battery", () -> new BatteryItem(new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> FLASHLIGHT = ITEMS.register(
            "flashlight", () -> new FlashlightItem(new Item.Properties().stacksTo(1))
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
