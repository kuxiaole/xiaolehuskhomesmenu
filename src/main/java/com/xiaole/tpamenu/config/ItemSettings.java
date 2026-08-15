package com.xiaole.tpamenu.config;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

public record ItemSettings(Material material, String name, List<String> lore) {
    public ItemSettings {
        material = Objects.requireNonNull(material, "material");
        name = Objects.requireNonNullElse(name, "");
        lore = List.copyOf(lore == null ? List.of() : lore);
    }
}
