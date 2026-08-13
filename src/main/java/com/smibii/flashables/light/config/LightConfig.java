package com.smibii.flashables.light.config;

import com.google.gson.Gson;
import com.smibii.flashables.helper.Logger;
import com.smibii.flashables.light.LightState;
import com.smibii.flashables.light.config.builtin.BuiltInConfigData;
import com.smibii.flashables.light.config.data.ConfigData;
import com.smibii.flashables.light.config.data.ListItemConfigData;
import com.smibii.flashables.light.config.data.StateConfigData;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class LightConfig {
    private final ConfigData config;
    private final LightType type;
    private final ActivationType activation;
    private final List<EquipmentSlot> slots;
    private final ItemStack item;
    private final Map<String, IntTag> tags;
    private final Vector3f color;
    private final float intensity;
    private final float radius;
    private final boolean shadows;
    private final boolean volumetric;
    private final float volumetricStrength;
    private final float volumetricStep;
    private final float angle;
    private final ResourceLocation texture;
    private final Map<String, LightState> states;
    private final boolean isBuiltIn;

    public LightConfig(ConfigData config) {
        this.config = config;
        type = LightType.fromString(config.type());
        activation = ActivationType.fromString(config.activation());
        List<String> esa = Arrays.stream(EquipmentSlot.values()).map(EquipmentSlot::getName).toList();
        if (config.slots() == null) throw new IllegalStateException("Expected $.slots with type EQUIPMENT_SLOT_ARRAY where item is one or more of " + esa);
        List<EquipmentSlot> slots = new ArrayList<>();
        for (String slot : config.slots()) {
            try {
                slots.add(EquipmentSlot.byName(slot));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Expected $.slots[" + Arrays.stream(config.slots()).toList().indexOf(slot) + "] with type EQUIPMENT_SLOT like one of " + esa);
            }
        }
        this.slots = slots;
        if (config.item() == null) throw new IllegalStateException("Expected $.item with type ITEM_RESOURCE_LOCATION but was NULL");
        ResourceLocation location = ResourceLocation.parse(config.item());
        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item == null) throw new IllegalStateException("Expected $.item with type ITEM_RESOURCE_LOCATION but was STRING");
        ItemStack itemStack = new ItemStack(item);
        if (itemStack.getTag() != null) {
            for (Map.Entry<String, IntTag> tag : tags().entrySet()) {
                itemStack.getTag().putInt(tag.getKey(), tag.getValue().getAsInt());
            }
        }
        this.item = itemStack;
        if (config.tags() != null) {
            Map<String, IntTag> tagsMap = new HashMap<>();
            for (ListItemConfigData<Integer> tag : config.tags()) {
                if (tag.name() == null) throw new IllegalStateException("Expected $.item[" + List.of(config.tags()).indexOf(tag) + "].name with type STRING but was NULL");
                if (tag.value() == null) throw new IllegalStateException("Expected $.item[" + List.of(config.tags()).indexOf(tag) + "].value with type INTEGER but was NULL");
                tagsMap.put(tag.name(), IntTag.valueOf(tag.value()));
            }
            tags = tagsMap;
        } else tags = null;
        if (config.color() == null) throw new IllegalStateException("Expected $.color with type COLOR_ARRAY with 3 FLOAT inputs but was NULL");
        if (config.color().length < 3) throw new IllegalStateException("Expected $.color with type COLOR_ARRAY with 3 FLOAT inputs but only had " + config.color().length);
        else color = new Vector3f(config.color()[0], config.color()[1], config.color()[2]);
        intensity = config.intensity();
        radius = config.radius();
        shadows = config.shadows();
        volumetric = config.volumetric();
        volumetricStrength = config.volumetric_strength();
        volumetricStep = config.volumetric_step();
        angle = config.angle();
        texture = ResourceLocation.parse(config.texture());
        if (config.states() == null) {
            Map<String, LightState> statesMap = getStringLightStateMap(config);
            states = statesMap;
        } else states = null;
        this.isBuiltIn = false;
    }

    private static @NotNull Map<String, LightState> getStringLightStateMap(ConfigData config) {
        Map<String, LightState> statesMap = new HashMap<>();
        for (StateConfigData stateItem : config.states()) {
            String name = stateItem.name();
            ListItemConfigData<String>[] properties = stateItem.properties();
            LightState state = new LightState();
            for (ListItemConfigData<String> property : properties) {
                state.add(property.name(), property.value());
            }
            statesMap.put(name, state);
        }
        return statesMap;
    }

    public LightConfig(BuiltInConfigData config) {
        this.config = null;
        type = config.type();
        activation = config.activation();
        slots = config.slots();
        item = config.item();
        tags = config.tags();
        color = config.color();
        intensity = config.intensity();
        radius = config.radius();
        shadows = config.shadows();
        volumetric = config.volumetric();
        volumetricStrength = config.volumetricStrength();
        volumetricStep = config.volumetricStep();
        angle = config.angle();
        texture = config.texture();
        states = config.states();
        this.isBuiltIn = true;
    }

    public Object property(String property) {
        if (!BuiltInConfigData.properties().contains(property)) return null;
        return switch (property) {
            case "type" -> this.type;
            case "activation" -> this.activation;
            case "slots" -> this.slots;
            case "item" -> this.item;
            case "tags" -> this.tags;
            case "color" -> this.color;
            case "intensity" -> this.intensity;
            case "radius" -> this.radius;
            case "shadows" -> this.shadows;
            case "volumetric" -> this.volumetric;
            case "volumetricStrength" -> this.volumetricStrength;
            case "volumetricStep" -> this.volumetricStep;
            case "angle" -> this.angle;
            case "texture" -> this.texture;
            case "states" -> this.states;
            default -> null;
        };
    }

    public ConfigData config() {
        return this.config;
    }

    public LightType type() {
        return this.type;
    }

    public ActivationType activation() {
        return this.activation;
    }

    public List<EquipmentSlot> slots() {
        return this.slots;
    }

    public ItemStack item() {
        return this.item;
    }

    public Map<String, IntTag> tags() {
        return this.tags;
    }

    public Vector3f color() {
        return this.color;
    }

    public float intensity() {
        return this.intensity;
    }

    public float radius() {
        return this.radius;
    }

    public boolean shadows() {
        return this.shadows;
    }

    public boolean volumetric() {
        return this.volumetric;
    }

    public float volumetricStrength() {
        return this.volumetricStrength;
    }

    public float volumetricStep() {
        return this.volumetricStep;
    }

    public float angle() {
        return this.angle;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public Map<String, LightState> states() {
        return this.states;
    }

    public boolean isBuiltIn() {
        return this.isBuiltIn;
    }

    public static LightConfig parse(Path file) {
        Gson gson = new Gson();
        String content = "{}";

        try {
            content = Files.readString(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ConfigData config = gson.fromJson(content, ConfigData.class);
        return new LightConfig(config);
    }

    public enum LightType {
        SPOT("spot"),
        POINT("point");

        private final String type;
        LightType(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }

        public static LightType fromString(String name) {
            for (LightType lightType : values()) {
                if (lightType.type.equals(name)) return lightType;
            }
            List<String> esa = Arrays.stream(LightType.values()).map(LightType::getType).toList();
            throw new IllegalStateException("Expected $.slots with type ACTION_TYPE like on of " + esa);
        }
    }

    public enum ActivationType {
        CLICK("click"),
        HOLD("hold");

        public final String type;
        ActivationType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static ActivationType fromString(String type) {
            for (ActivationType activationType : ActivationType.values()) {
                if (activationType.type.equals(type)) return activationType;
            }
            List<String> esa = Arrays.stream(ActivationType.values()).map(ActivationType::getType).toList();
            throw new IllegalStateException("Expected $.slots with type ACTION_TYPE like on of " + esa);
        }
    }
}
