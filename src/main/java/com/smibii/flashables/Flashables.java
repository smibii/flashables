package com.smibii.flashables;

import com.smibii.flashables.client.render.LightBuffer;
import com.smibii.flashables.light.LightManager;
import com.smibii.flashables.light.types.PointLight;
import com.smibii.flashables.registry.CreativeTab;
import com.smibii.flashables.registry.FlashablesItems;
import com.smibii.flashables.registry.crafting.RecipeSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
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

        PointLight light = new PointLight()
                .position(-24, 93, -2)
                .enabled(true);
        LightManager.add(light);
    }
}
