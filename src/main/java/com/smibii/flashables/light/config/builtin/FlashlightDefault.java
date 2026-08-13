package com.smibii.flashables.light.config.builtin;

import com.smibii.flashables.Flashables;
import com.smibii.flashables.FlashablesItems;
import com.smibii.flashables.light.LightState;
import com.smibii.flashables.light.config.LightConfig;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class FlashlightDefault extends BuiltInConfigData {
    private final String name;

    public FlashlightDefault(boolean isFirstPerson, int index, String colorName, Vector3f color) {
        super();
        set("type", LightConfig.LightType.SPOT);
        set("activation", LightConfig.ActivationType.CLICK);
        set("slots", List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
        set("item", new ItemStack(FlashablesItems.FLASHLIGHT.get()));
        set("tags", Map.of("Color", IntTag.valueOf(index)));
        set("color", color);
        set("radius", 20.0f);
        set("angle", 30.0f);
        set("texture", Flashables.location("textures/light/flashlight.png"));
        set("states", Map.of(
                "flashing",
                new LightState().add("intensity", "1 - cos(time + pi) * 0.5")));

        colorName += "_";
        if (isFirstPerson) {
            name = colorName + "first_person_" + "flashlight.json";
            set("volumetricStrength", 0.02f);
        } else name = colorName + "flashlight.json";
    }

    @Override
    public String file() {
        return name;
    }
}
