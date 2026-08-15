package com.xiaole.tpamenu.listener;

import com.xiaole.tpamenu.menu.WarpMenuHolder;
import com.xiaole.tpamenu.menu.WarpMenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class WarpMenuListener implements Listener {
    private final WarpMenuService menuService;

    public WarpMenuListener(WarpMenuService menuService) {
        this.menuService = menuService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        menuService.cancelPendingOpen(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof WarpMenuHolder holder)) {
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

        menuService.handleAction(
                player,
                action,
                holder.page(),
                menuService.readWarpIdentifier(item),
                menuService.readWarpName(item)
        );
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WarpMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof WarpMenuHolder && event.getPlayer() instanceof Player player) {
            menuService.cancelPendingOpen(player.getUniqueId());
        }
    }
}
