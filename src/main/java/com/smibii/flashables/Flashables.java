package com.smibii.flashables;

import com.smibii.flashables.crafting.RecipeSerializers;
import com.smibii.flashables.light.config.ConfigManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Flashables.MODID)
public class Flashables {
    public static final String MODID = "flashables";

    public static ResourceLocation location(String path) {
        return new ResourceLocation(MODID, path);
    }

    public Flashables() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        FlashablesItems.register(bus);
        CreativeTab.register(bus);
        RecipeSerializers.register(bus);
    }
}
