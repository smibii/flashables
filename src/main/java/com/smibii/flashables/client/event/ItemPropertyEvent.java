package com.smibii.flashables.client.event;

import com.smibii.flashables.Flashables;
import com.smibii.flashables.registry.FlashablesItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemPropertyEvent {
    private ItemPropertyEvent() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    FlashablesItems.FLASHLIGHT.get(),
                    Flashables.location("color"),
                    (ItemStack stack, ClientLevel level, LivingEntity entity, int seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("Color")) {
                            return stack.getTag().getInt("Color");
                        }
                        return 0.0f;
                    }
            );
        });
    }
}
