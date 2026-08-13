package com.smibii.flashables.light.config.builtin;

import com.smibii.flashables.helper.Logger;
import com.smibii.flashables.light.LightState;
import com.smibii.flashables.light.config.LightConfig;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public class BuiltInConfigData {

    private final Map<String, Consumer<Object>> properties = new HashMap<>();

    protected String file = "";
    protected LightConfig.LightType type = LightConfig.LightType.POINT;
    protected LightConfig.ActivationType activation = LightConfig.ActivationType.HOLD;
    protected List<EquipmentSlot> slots =
            List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

    protected ItemStack item = null;
    protected Map<String, IntTag> tags = new HashMap<>();
    protected Vector3f color = new Vector3f(1f, 1f, 1f);

    protected float intensity = 1.0f;
    protected float radius = 10.0f;

    protected boolean shadows = false;
    protected boolean volumetric = true;

    protected float volumetricStrength = 0.035f;
    protected float volumetricStep = 32.0f;

    protected float angle = 45.0f;

    protected ResourceLocation texture = null;

    protected Map<String, LightState> states = new HashMap<>();

    public BuiltInConfigData() {
        registerProperties();
    }

    public void set(String property, Object value) {
        Consumer<Object> consumer = properties.get(property);

        if (consumer == null) {
            return;
        }

        try {
            consumer.accept(value);
        }  catch (Exception e) {
            Logger.error("Failed to set property", property + ":", e.getMessage());
        }
    }

    private void registerProperties() {
        property("type", value -> this.type = (LightConfig.LightType) value);
        property("activation", value -> this.activation = (LightConfig.ActivationType) value);
        property("slots", value -> this.slots = (List<EquipmentSlot>) value);
        property("item", value -> this.item = (ItemStack) value);
        property("tags", value -> this.tags = (Map<String, IntTag>) value);
        property("color", value -> this.color = (Vector3f) value);
        property("intensity", value -> this.intensity = ((Number) value).floatValue());
        property("radius", value -> this.radius = ((Number) value).floatValue());
        property("shadows", value -> this.shadows = (Boolean) value);
        property("volumetric", value -> this.volumetric = (Boolean) value);
        property("volumetricStrength", value -> this.volumetricStrength = ((Number) value).floatValue() );
        property("volumetricStep", value -> this.volumetricStep = ((Number) value).floatValue());
        property("angle", value -> this.angle = ((Number) value).floatValue());
        property("texture", value -> this.texture = (ResourceLocation) value);
        property("states", value -> this.states = (Map<String, LightState>) value);
    }

    private void property(
            String name,
            Consumer<Object> setter
    ) {
        properties.put(name, setter);
    }

    public static Set<String> properties() {
        return new BuiltInConfigData().properties.keySet();
    }

    public String file() {
        return file;
    }

    public LightConfig.LightType type() {
        return type;
    }

    public LightConfig.ActivationType activation() {
        return activation;
    }

    public List<EquipmentSlot> slots() {
        return slots;
    }

    public ItemStack item() {
        return item;
    }

    public Map<String, IntTag> tags() {
        return tags;
    }

    public Vector3f color() {
        return color;
    }

    public float intensity() {
        return intensity;
    }

    public float radius() {
        return radius;
    }

    public boolean shadows() {
        return shadows;
    }

    public boolean volumetric() {
        return volumetric;
    }

    public float volumetricStrength() {
        return volumetricStrength;
    }

    public float volumetricStep() {
        return volumetricStep;
    }

    public float angle() {
        return angle;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public Map<String, LightState> states() {
        return states;
    }
}