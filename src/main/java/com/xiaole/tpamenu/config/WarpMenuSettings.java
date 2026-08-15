package com.xiaole.tpamenu.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WarpMenuSettings(
        String title,
        int size,
        int warpSlots,
        boolean closeAfterTeleport,
        String warpCommand,
        Duration loadTimeout,
        String noDescription,
        ItemSettings warpItem,
        Map<String, ItemSettings> warpOverrides,
        ItemSettings previousItem,
        ItemSettings nextItem,
        ItemSettings refreshItem,
        ItemSettings closeItem,
        ItemSettings infoItem,
        ItemSettings emptyItem,
        ItemSettings fillerItem
) {
    public WarpMenuSettings {
        warpOverrides = Map.copyOf(warpOverrides);
    }

    public static WarpMenuSettings from(FileConfiguration config) {
        int size = MenuSettings.clampInventorySize(config.getInt("warp-menu.size", 54));
        int warpSlots = Math.max(1, Math.min(config.getInt("warp-menu.warp-slots", 45), size - 9));
        ItemSettings warpItem = item(
                config,
                "warp-menu.items.warp",
                new ItemSettings(
                        Material.ENDER_PEARL,
                        "&a{warp}",
                        List.of("&7{description}", "", "&e点击传送")
                )
        );

        return new WarpMenuSettings(
                config.getString("warp-menu.title", "&0地标传送菜单 &8({page}/{pages})"),
                size,
                warpSlots,
                config.getBoolean("warp-menu.close-after-teleport", true),
                config.getString("warp-menu.warp-command", "huskhomes:warp {warp}"),
                Duration.ofMillis(Math.max(1, config.getLong("warp-menu.load-timeout-millis", 5000))),
                config.getString("warp-menu.no-description", "暂无描述"),
                warpItem,
                loadOverrides(config.getConfigurationSection("warp-menu.warps"), warpItem),
                item(config, "warp-menu.items.previous", new ItemSettings(Material.ARROW, "&e上一页", List.of("&7当前: &f{page}&7/&f{pages}"))),
                item(config, "warp-menu.items.next", new ItemSettings(Material.ARROW, "&e下一页", List.of("&7当前: &f{page}&7/&f{pages}"))),
                item(config, "warp-menu.items.refresh", new ItemSettings(Material.CLOCK, "&b刷新地标", List.of())),
                item(config, "warp-menu.items.close", new ItemSettings(Material.BARRIER, "&c关闭", List.of())),
                item(config, "warp-menu.items.info", new ItemSettings(Material.MAP, "&f地标数量: &a{count}", List.of("&7第 &f{page}&7/&f{pages} &7页"))),
                item(config, "warp-menu.items.empty", new ItemSettings(Material.GRAY_DYE, "&7暂无可用地标", List.of())),
                item(config, "warp-menu.items.filler", new ItemSettings(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()))
        );
    }

    public ItemSettings itemFor(String warpName) {
        return warpOverrides.getOrDefault(warpName, warpItem);
    }

    private static Map<String, ItemSettings> loadOverrides(ConfigurationSection section, ItemSettings fallback) {
        if (section == null) {
            return Map.of();
        }

        Map<String, ItemSettings> overrides = new LinkedHashMap<>();
        for (String warpName : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(warpName);
            if (itemSection != null) {
                overrides.put(warpName, item(itemSection, fallback));
            }
        }
        return overrides;
    }

    private static ItemSettings item(FileConfiguration config, String path, ItemSettings fallback) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section == null ? fallback : item(section, fallback);
    }

    private static ItemSettings item(ConfigurationSection section, ItemSettings fallback) {
        Material material = MenuSettings.validItemMaterial(section.getString("material", fallback.material().name()));
        List<String> lore = section.contains("lore") ? section.getStringList("lore") : fallback.lore();
        return new ItemSettings(
                material == null ? fallback.material() : material,
                section.getString("name", fallback.name()),
                lore
        );
    }
}
