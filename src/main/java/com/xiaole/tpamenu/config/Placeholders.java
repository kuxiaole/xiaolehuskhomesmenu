package com.xiaole.tpamenu.config;

import java.util.HashMap;
import java.util.Map;

public final class Placeholders {
    private final Map<String, String> values = new HashMap<>();

    public Placeholders set(String key, Object value) {
        values.put(key, String.valueOf(value));
        return this;
    }

    public String apply(String input) {
        String output = input == null ? "" : input;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return output;
    }
}
