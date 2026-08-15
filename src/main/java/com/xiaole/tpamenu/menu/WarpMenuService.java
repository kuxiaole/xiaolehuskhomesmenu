package com.xiaole.tpamenu.menu;

import com.xiaole.tpamenu.config.ItemSettings;
import com.xiaole.tpamenu.config.MenuSettings;
import com.xiaole.tpamenu.config.Placeholders;
import com.xiaole.tpamenu.config.WarpMenuSettings;
import com.xiaole.tpamenu.scheduler.SchedulerUtil;
import com.xiaole.tpamenu.warp.WarpEntry;
import com.xiaole.tpamenu.warp.WarpProvider;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class WarpMenuService {
    private static final String ACTION_WARP = "warp";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_REFRESH = "refresh";
    private static final String ACTION_CLOSE = "close";

    private final Plugin plugin;
    private final SchedulerUtil scheduler;
    private final WarpProvider warpProvider;
    private final NamespacedKey actionKey;
    private final NamespacedKey warpIdentifierKey;
    private final NamespacedKey warpNameKey;
    private final AtomicLong openSequence = new AtomicLong();
    private final ConcurrentHashMap<UUID, Long> pendingOpens = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile MenuSettings settings;

    public WarpMenuService(
            Plugin plugin,
            SchedulerUtil scheduler,
            WarpProvider warpProvider,
            MenuSettings settings
    ) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.warpProvider = warpProvider;
        this.settings = settings;
        this.actionKey = new NamespacedKey(plugin, "warp_action");
        this.warpIdentifierKey = new NamespacedKey(plugin, "warp_identifier");
        this.warpNameKey = new NamespacedKey(plugin, "warp_name");
    }

    public void update(MenuSettings settings) {
        this.settings = settings;
        pendingOpens.clear();
    }

    public void open(Player viewer, int requestedPage) {
        MenuSettings currentSettings = settings;
        WarpMenuSettings warpSettings = currentSettings.warpMenu();
        if (!warpProvider.available() || !plugin.getServer().getPluginManager().isPluginEnabled("HuskHomes")) {
            currentSettings.send(viewer, "messages.huskhomes-missing");
            return;
        }

        UUID viewerId = viewer.getUniqueId();
        long openId = openSequence.incrementAndGet();
        pendingOpens.put(viewerId, openId);
        currentSettings.send(viewer, "messages.warp-opening");

        CompletableFuture<List<WarpEntry>> warps;
        try {
            warps = warpProvider.loadWarps();
        } catch (LinkageError | RuntimeException exception) {
            pendingOpens.remove(viewerId, openId);
            currentSettings.send(viewer, "messages.warp-load-failed");
            return;
        }

        warps.orTimeout(warpSettings.loadTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((entries, throwable) -> completeOpen(
                        viewer,
                        viewerId,
                        openId,
                        requestedPage,
                        entries,
                        throwable,
                        currentSettings
                ));
    }

    public void handleAction(
            Player viewer,
            String action,
            int currentPage,
            String warpIdentifier,
            String warpName
    ) {
        scheduler.runAtEntityDelayed(viewer, () -> {
            if (!viewer.isOnline()) {
                return;
            }
            Inventory topInventory = viewer.getOpenInventory().getTopInventory();
            if (!(topInventory.getHolder() instanceof WarpMenuHolder holder)
                    || !holder.viewerId().equals(viewer.getUniqueId())
                    || holder.page() != currentPage) {
                return;
            }
            handleActionNow(viewer, action, currentPage, warpIdentifier, warpName);
        }, 1);
    }

    private void handleActionNow(
            Player viewer,
            String action,
            int currentPage,
            String warpIdentifier,
            String warpName
    ) {
        switch (action) {
            case ACTION_WARP -> {
                cancelPendingOpen(viewer.getUniqueId());
                teleportToWarp(viewer, warpIdentifier, warpName);
            }
            case ACTION_PREVIOUS -> open(viewer, currentPage - 1);
            case ACTION_NEXT -> open(viewer, currentPage + 1);
            case ACTION_REFRESH -> open(viewer, currentPage);
            case ACTION_CLOSE -> {
                cancelPendingOpen(viewer.getUniqueId());
                viewer.closeInventory();
            }
            default -> {
            }
        }
    }

    public void cancelPendingOpen(UUID viewerId) {
        pendingOpens.remove(viewerId);
    }

    public void shutdown() {
        closed.set(true);
        pendingOpens.clear();
    }

    public String readAction(ItemStack item) {
        return readString(item, actionKey);
    }

    public String readWarpIdentifier(ItemStack item) {
        return readString(item, warpIdentifierKey);
    }

    public String readWarpName(ItemStack item) {
        return readString(item, warpNameKey);
    }

    private void completeOpen(
            Player viewer,
            UUID viewerId,
            long openId,
            int requestedPage,
            List<WarpEntry> entries,
            Throwable throwable,
            MenuSettings currentSettings
    ) {
        if (closed.get() || !Long.valueOf(openId).equals(pendingOpens.get(viewerId))) {
            return;
        }

        scheduler.runAtEntity(viewer, () -> {
            if (closed.get() || !pendingOpens.remove(viewerId, openId) || !viewer.isOnline()) {
                return;
            }
            if (throwable != null || entries == null) {
                currentSettings.send(viewer, "messages.warp-load-failed");
                return;
            }

            List<WarpEntry> safeEntries = List.copyOf(entries);
            WarpMenuSettings warpSettings = currentSettings.warpMenu();
            PageSlice<WarpEntry> page = PageSlice.of(safeEntries, requestedPage, warpSettings.warpSlots());
            Placeholders placeholders = pagePlaceholders(page.index(), page.totalPages(), safeEntries.size());
            viewer.openInventory(buildInventory(viewerId, page, safeEntries.size(), placeholders, currentSettings));
        });
    }

    private Inventory buildInventory(
            UUID viewerId,
            PageSlice<WarpEntry> page,
            int totalWarps,
            Placeholders placeholders,
            MenuSettings currentSettings
    ) {
        WarpMenuSettings warpSettings = currentSettings.warpMenu();
        WarpMenuHolder holder = new WarpMenuHolder(viewerId, page.index());
        Inventory inventory = Bukkit.createInventory(
                holder,
                warpSettings.size(),
                currentSettings.text(warpSettings.title(), placeholders)
        );
        holder.inventory(inventory);

        fill(inventory, warpSettings.fillerItem(), placeholders, currentSettings);
        for (int index = 0; index < page.entries().size(); index++) {
            inventory.setItem(index, warpItem(page.entries().get(index), currentSettings));
        }

        if (page.entries().isEmpty()) {
            inventory.setItem(
                    Math.min(22, warpSettings.warpSlots() - 1),
                    simpleItem(warpSettings.emptyItem(), placeholders, null, currentSettings)
            );
        }

        int bottom = warpSettings.size() - 9;
        if (page.hasPrevious()) {
            inventory.setItem(bottom, simpleItem(warpSettings.previousItem(), placeholders, ACTION_PREVIOUS, currentSettings));
        }
        inventory.setItem(bottom + 3, simpleItem(warpSettings.refreshItem(), placeholders, ACTION_REFRESH, currentSettings));
        inventory.setItem(
                bottom + 4,
                simpleItem(warpSettings.infoItem(), placeholders.set("count", totalWarps), null, currentSettings)
        );
        inventory.setItem(bottom + 5, simpleItem(warpSettings.closeItem(), placeholders, ACTION_CLOSE, currentSettings));
        if (page.hasNext()) {
            inventory.setItem(bottom + 8, simpleItem(warpSettings.nextItem(), placeholders, ACTION_NEXT, currentSettings));
        }
        return inventory;
    }

    private ItemStack warpItem(WarpEntry entry, MenuSettings currentSettings) {
        WarpMenuSettings warpSettings = currentSettings.warpMenu();
        ItemSettings itemSettings = warpSettings.itemFor(entry.name());
        String description = entry.description().isBlank() ? warpSettings.noDescription() : entry.description();
        Placeholders placeholders = new Placeholders()
                .set("warp", entry.name())
                .set("description", description);

        ItemStack item = new ItemStack(itemSettings.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(currentSettings.text(itemSettings.name(), placeholders));
        meta.lore(currentSettings.lore(itemSettings.lore(), placeholders));
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(actionKey, PersistentDataType.STRING, ACTION_WARP);
        data.set(warpIdentifierKey, PersistentDataType.STRING, entry.safeIdentifier());
        data.set(warpNameKey, PersistentDataType.STRING, entry.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack simpleItem(
            ItemSettings itemSettings,
            Placeholders placeholders,
            String action,
            MenuSettings currentSettings
    ) {
        ItemStack item = new ItemStack(itemSettings.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(currentSettings.text(itemSettings.name(), placeholders));
        meta.lore(currentSettings.lore(itemSettings.lore(), placeholders));
        if (action != null) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fill(
            Inventory inventory,
            ItemSettings filler,
            Placeholders placeholders,
            MenuSettings currentSettings
    ) {
        ItemStack item = simpleItem(filler, placeholders, null, currentSettings);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    private void teleportToWarp(Player viewer, String warpIdentifier, String warpName) {
        if (warpIdentifier == null || warpIdentifier.isBlank()) {
            return;
        }

        MenuSettings currentSettings = settings;
        if (!plugin.getServer().getPluginManager().isPluginEnabled("HuskHomes")) {
            currentSettings.send(viewer, "messages.huskhomes-missing");
            return;
        }

        if (currentSettings.warpMenu().closeAfterTeleport()) {
            viewer.closeInventory();
        }

        String command = currentSettings.warpMenu().warpCommand()
                .replace("{warp}", warpIdentifier)
                .replace("{name}", warpName == null ? warpIdentifier : warpName)
                .trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank() || !viewer.performCommand(command)) {
            currentSettings.send(viewer, "messages.warp-command-failed");
        }
    }

    private String readString(ItemStack item, NamespacedKey key) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private Placeholders pagePlaceholders(int page, int totalPages, int count) {
        return new Placeholders()
                .set("page", page + 1)
                .set("pages", totalPages)
                .set("count", count);
    }
}
