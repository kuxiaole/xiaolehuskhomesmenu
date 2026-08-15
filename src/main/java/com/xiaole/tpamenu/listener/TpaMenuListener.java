package com.xiaole.tpamenu.listener;

import com.xiaole.tpamenu.menu.OnlinePlayerRegistry;
import com.xiaole.tpamenu.menu.TpaMenuHolder;
import com.xiaole.tpamenu.menu.TpaMenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class TpaMenuListener implements Listener {
    private final TpaMenuService menuService;
    private final OnlinePlayerRegistry onlinePlayers;

    public TpaMenuListener(TpaMenuService menuService, OnlinePlayerRegistry onlinePlayers) {
        this.menuService = menuService;
        this.onlinePlayers = onlinePlayers;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        onlinePlayers.track(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        onlinePlayers.untrack(player.getUniqueId());
        menuService.cancelPendingOpen(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof TpaMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.viewerId().equals(player.getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        String action = menuService.readAction(item);
        if (action == null) {
            return;
        }

        menuService.handleAction(player, action, holder.page(), menuService.readTargetName(item));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TpaMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TpaMenuHolder && event.getPlayer() instanceof Player player) {
            menuService.cancelPendingOpen(player.getUniqueId());
        }
    }
}
