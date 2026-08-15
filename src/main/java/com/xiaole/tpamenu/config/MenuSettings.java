package com.xiaole.tpamenu.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public record MenuSettings(
        String title,
        int size,
        int playerSlots,
        boolean showSelf,
        boolean closeAfterRequest,
        String tpaCommand,
        Duration skinFetchTimeout,
        Duration skinCacheTtl,
        int skinThreads,
        ItemSettings playerItem,
        ItemSettings previousItem,
        ItemSettings nextItem,
        ItemSettings refreshItem,
        ItemSettings closeItem,
        ItemSettings infoItem,
        ItemSettings emptyItem,
        ItemSettings fillerItem,
        WarpMenuSettings warpMenu,
        String prefix,
        FileConfiguration config
) {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static MenuSettings from(FileConfiguration config) {
        int size = clampInventorySize(config.getInt("menu.size", 54));
        int playerSlots = Math.max(1, Math.min(config.getInt("menu.player-slots", 45), size - 9));

        return new MenuSettings(
                config.getString("menu.title", "&0传送请求菜单 &8({page}/{pages})"),
                size,
                playerSlots,
                config.getBoolean("menu.show-self", false),
                config.getBoolean("menu.close-after-request", true),
                config.getString("menu.tpa-command", "tpa {player}"),
                Duration.ofMillis(Math.max(0, config.getLong("menu.skin-fetch-timeout-millis", 1500))),
                Duration.ofMinutes(Math.max(1, config.getLong("menu.skin-cache-minutes", 15))),
                Math.max(1, Math.min(8, config.getInt("menu.skin-threads", 2))),
                item(config, "items.player", Material.PLAYER_HEAD, "&a{player}", List.of("&7点击发送传送请求")),
                item(config, "items.previous", Material.ARROW, "&e上一页", List.of("&7当前: &f{page}&7/&f{pages}")),
                item(config, "items.next", Material.ARROW, "&e下一页", List.of("&7当前: &f{page}&7/&f{pages}")),
                item(config, "items.refresh", Material.CLOCK, "&b刷新", List.of()),
                item(config, "items.close", Material.BARRIER, "&c关闭", List.of()),
                item(config, "items.info", Material.PAPER, "&f在线玩家: &a{count}", List.of("&7第 &f{page}&7/&f{pages} &7页")),
                item(config, "items.empty", Material.GRAY_DYE, "&7暂无可传送玩家", List.of()),
                item(config, "items.filler", Material.BLACK_STAINED_GLASS_PANE, " ", List.of()),
                WarpMenuSettings.from(config),
                config.getString("messages.prefix", ""),
                config
        );
    }

    public Component text(String raw) {
        return LEGACY.deserialize(raw == null ? "" : raw);
    }

    public Component text(String raw, Placeholders placeholders) {
        return text(placeholders.apply(raw));
    }

    public List<Component> lore(List<String> lines, Placeholders placeholders) {
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(text(placeholders.apply(line)));
        }
        return components;
    }

    public void send(CommandSender sender, String path) {
        String message = config.getString(path, "");
        if (message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(text(prefix + message));
    }

    public void send(CommandSender sender, String path, Placeholders placeholders) {
        String message = config.getString(path, "");
        if (message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(text(placeholders.apply(prefix + message)));
    }

    static ItemSettings item(FileConfiguration config, String path, Material fallback, String fallbackName, List<String> fallbackLore) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return new ItemSettings(fallback, fallbackName, fallbackLore);
        }

        Material material = validItemMaterial(section.getString("material", fallback.name()));
        if (material == null) {
            material = fallback;
        }

        return new ItemSettings(
                material,
                section.getString("name", fallbackName),
                section.getStringList("lore").isEmpty() ? fallbackLore : section.getStringList("lore")
        );
    }

    static Material validItemMaterial(String name) {
        Material material = name == null ? null : Material.matchMaterial(name);
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return null;
        }
        return material;
    }

    static int clampInventorySize(int requested) {
        int size = Math.max(18, Math.min(54, requested));
        return ((size + 8) / 9) * 9;
    }
}
