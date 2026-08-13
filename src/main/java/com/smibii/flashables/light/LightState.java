package com.smibii.flashables.light;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LightState {
    private final Map<String, Expression> properties = new HashMap<>();

    public LightState add(String property, String expression) {
        properties.put(property, Expression.compile(expression));
        return this;
    }

    public void tick(Light<?> light, double time) {
        for (var entry : properties.entrySet()) {
            float value = (float) entry.getValue().evaluate(time);
            light.setProperty(entry.getKey(), value);
        }
    }
}
