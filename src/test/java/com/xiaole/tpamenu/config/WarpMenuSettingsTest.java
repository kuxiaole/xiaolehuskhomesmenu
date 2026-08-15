package com.xiaole.tpamenu.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WarpMenuSettingsTest {
    @Test
    void warpOverrideInheritsMissingDefaultFields() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("warp-menu.items.warp.material", "COMPASS");
        config.set("warp-menu.items.warp.name", "&a{warp}");
        config.set("warp-menu.items.warp.lore", List.of("&7{description}", "&e点击传送"));
        config.set("warp-menu.warps.spawn.material", "GRASS_BLOCK");

        WarpMenuSettings settings = WarpMenuSettings.from(config);
        ItemSettings spawn = settings.itemFor("spawn");

        assertEquals(Material.GRASS_BLOCK, spawn.material());
        assertEquals("&a{warp}", spawn.name());
        assertEquals(List.of("&7{description}", "&e点击传送"), spawn.lore());
        assertEquals(Material.COMPASS, settings.itemFor("unknown").material());
    }

    @Test
    void invalidOverrideMaterialFallsBackToDefaultIcon() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("warp-menu.items.warp.material", "ENDER_PEARL");
        config.set("warp-menu.warps.market.material", "NOT_A_MATERIAL");
        config.set("warp-menu.warps.market.lore", List.of("&7自定义市场描述"));

        ItemSettings market = WarpMenuSettings.from(config).itemFor("market");

        assertEquals(Material.ENDER_PEARL, market.material());
        assertEquals(List.of("&7自定义市场描述"), market.lore());
    }

    @Test
    void bundledChineseConfigLoadsWarpDefaultsAndOverride() {
        InputStream input = getClass().getResourceAsStream("/config.yml");
        assertNotNull(input);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        );

        WarpMenuSettings settings = WarpMenuSettings.from(config);

        assertEquals(54, settings.size());
        assertEquals(45, settings.warpSlots());
        assertEquals(Material.ENDER_PEARL, settings.warpItem().material());
        assertEquals(Material.ENDER_PEARL, settings.itemFor("spawn").material());
        assertEquals(0, settings.warpOverrides().size());
    }
}
