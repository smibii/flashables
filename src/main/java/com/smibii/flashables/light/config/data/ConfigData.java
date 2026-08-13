package com.smibii.flashables.light.config.data;

public record ConfigData(
        String type,
        String activation,
        String[] slots,

        String item,
        ListItemConfigData<Integer>[] tags,

        float[] color,
        float intensity,
        float radius,

        boolean shadows,
        boolean volumetric,
        float volumetric_strength,
        float volumetric_step,

        float angle,
        String texture,

        StateConfigData[] states
) {}
