package com.xiaole.tpamenu.command;

import com.xiaole.tpamenu.TpaMenuPlugin;
import com.xiaole.tpamenu.menu.WarpMenuService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class WarpMenuCommand implements CommandExecutor, TabCompleter {
    private final TpaMenuPlugin plugin;
    private final WarpMenuService menuService;

    public WarpMenuCommand(TpaMenuPlugin plugin, WarpMenuService menuService) {
        this.plugin = plugin;
        this.menuService = menuService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("tpamenu.reload")) {
                plugin.settings().send(sender, "messages.no-permission");
                return true;
            }
            plugin.reloadPlugin();
            plugin.settings().send(sender, "messages.reloaded");
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.settings().send(sender, "messages.only-player");
            return true;
        }

        if (!player.hasPermission("tpamenu.warp.open")) {
            plugin.settings().send(player, "messages.no-permission");
            return true;
        }

        menuService.open(player, 0);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && sender.hasPermission("tpamenu.reload")
                && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}
