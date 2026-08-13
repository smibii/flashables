package com.smibii.flashables.light.config.data;

public record ListItemConfigData<T> (
    String name,
    T value
) {}
