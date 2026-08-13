package com.smibii.flashables.light.config.data;

public record StateConfigData(
        String name,
        ListItemConfigData<String>[] properties
) {}
