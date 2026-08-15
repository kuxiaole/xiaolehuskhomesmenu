package com.xiaole.tpamenu.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class WarpMenuHolder implements InventoryHolder {
    private final UUID viewerId;
    private final int page;
    private Inventory inventory;

    public WarpMenuHolder(UUID viewerId, int page) {
        this.viewerId = viewerId;
        this.page = page;
    }

    public UUID viewerId() {
        return viewerId;
    }

    public int page() {
        return page;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
