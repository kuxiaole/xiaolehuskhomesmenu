package com.xiaole.tpamenu.menu;

import com.xiaole.tpamenu.config.ItemSettings;
import com.xiaole.tpamenu.config.MenuSettings;
import com.xiaole.tpamenu.config.Placeholders;
import com.xiaole.tpamenu.scheduler.SchedulerUtil;
import com.xiaole.tpamenu.skin.SkinProvider;
import com.xiaole.tpamenu.skin.SkinTexture;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TpaMenuService {
    private static final String ACTION_PLAYER = "player";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_REFRESH = "refresh";
    private static final String ACTION_CLOSE = "close";

    private final Plugin plugin;
    private final SchedulerUtil scheduler;
    private final OnlinePlayerRegistry onlinePlayers;
    private final NamespacedKey actionKey;
    private final NamespacedKey targetNameKey;
    private final NamespacedKey targetUuidKey;
    private final AtomicLong openSequence = new AtomicLong();
    private final ConcurrentHashMap<UUID, Long> pendingOpens = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile SkinProvider skinProvider;
    private volatile MenuSettings settings;

    public TpaMenuService(
            Plugin plugin,
            SchedulerUtil scheduler,
            OnlinePlayerRegistry onlinePlayers,
            SkinProvider skinProvider,
            MenuSettings settings
    ) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.onlinePlayers = onlinePlayers;
        this.skinProvider = skinProvider;
        this.settings = settings;
        this.actionKey = new NamespacedKey(plugin, "action");
        this.targetNameKey = new NamespacedKey(plugin, "target_name");
        this.targetUuidKey = new NamespacedKey(plugin, "target_uuid");
    }

    public void update(MenuSettings settings, SkinProvider skinProvider) {
        this.settings = settings;
        this.skinProvider = skinProvider;
        pendingOpens.clear();
    }

    public void open(Player viewer, int requestedPage) {
        MenuSettings currentSettings = settings;
        SkinProvider currentSkinProvider = skinProvider;
        UUID viewerId = viewer.getUniqueId();
        long openId = openSequence.incrementAndGet();
        pendingOpens.put(viewerId, openId);
        currentSettings.send(viewer, "messages.opening");

        List<PlayerEntry> players = onlinePlayers.snapshot(viewerId, currentSettings.showSelf());
        PageSlice<PlayerEntry> page = PageSlice.of(players, requestedPage, currentSettings.playerSlots());
        Placeholders placeholders = pagePlaceholders(page.index(), page.totalPages(), players.size());

        CompletableFuture<Map<UUID, SkinTexture>> textures;
        try {
            textures = currentSkinProvider.loadTextures(page.entries(), currentSettings.skinFetchTimeout());
        } catch (RuntimeException exception) {
            textures = CompletableFuture.completedFuture(Map.of());
        }

        textures.whenComplete((skinMap, throwable) -> {
            if (closed.get() || !Long.valueOf(openId).equals(pendingOpens.get(viewerId))) {
                return;
            }
            scheduler.runAtEntity(viewer, () -> {
                if (closed.get() || !pendingOpens.remove(viewerId, openId) || !viewer.isOnline()) {
                    return;
                }
                Map<UUID, SkinTexture> safeSkinMap = skinMap == null ? Map.of() : skinMap;
                Inventory inventory = buildInventory(
                        viewerId,
                        page,
                        players.size(),
                        safeSkinMap,
                        placeholders,
                        currentSettings
                );
                viewer.openInventory(inventory);
            });
        });
    }

    public void handleAction(Player viewer, String action, int currentPage, String targetName) {
        scheduler.runAtEntityDelayed(viewer, () -> {
            if (!viewer.isOnline()) {
                return;
            }
            Inventory topInventory = viewer.getOpenInventory().getTopInventory();
            if (!(topInventory.getHolder() instanceof TpaMenuHolder holder)
                    || !holder.viewerId().equals(viewer.getUniqueId())
                    || holder.page() != currentPage) {
                return;
            }
            handleActionNow(viewer, action, currentPage, targetName);
        }, 1);
    }

    private void handleActionNow(Player viewer, String action, int currentPage, String targetName) {
        switch (action) {
            case ACTION_PLAYER -> {
                cancelPendingOpen(viewer.getUniqueId());
                requestTpa(viewer, targetName);
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
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public String readTargetName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(targetNameKey, PersistentDataType.STRING);
    }

    private Inventory buildInventory(
            UUID viewerId,
            PageSlice<PlayerEntry> page,
            int totalPlayers,
            Map<UUID, SkinTexture> skins,
            Placeholders placeholders,
            MenuSettings currentSettings
    ) {
        TpaMenuHolder holder = new TpaMenuHolder(viewerId, page.index());
        Inventory inventory = Bukkit.createInventory(holder, currentSettings.size(), currentSettings.text(currentSettings.title(), placeholders));
        holder.inventory(inventory);

        fill(inventory, currentSettings.fillerItem(), placeholders, currentSettings);
        for (int index = 0; index < page.entries().size(); index++) {
            PlayerEntry entry = page.entries().get(index);
            inventory.setItem(index, playerItem(entry, skins.get(entry.uuid()), currentSettings));
        }

        if (page.entries().isEmpty()) {
            inventory.setItem(
                    Math.min(22, currentSettings.playerSlots() - 1),
                    simpleItem(currentSettings.emptyItem(), placeholders, null, currentSettings)
            );
        }

        int bottom = currentSettings.size() - 9;
        if (page.hasPrevious()) {
            inventory.setItem(bottom, simpleItem(currentSettings.previousItem(), placeholders, ACTION_PREVIOUS, currentSettings));
        }
        inventory.setItem(bottom + 3, simpleItem(currentSettings.refreshItem(), placeholders, ACTION_REFRESH, currentSettings));
        inventory.setItem(bottom + 4, simpleItem(currentSettings.infoItem(), placeholders.set("count", totalPlayers), null, currentSettings));
        inventory.setItem(bottom + 5, simpleItem(currentSettings.closeItem(), placeholders, ACTION_CLOSE, currentSettings));
        if (page.hasNext()) {
            inventory.setItem(bottom + 8, simpleItem(currentSettings.nextItem(), placeholders, ACTION_NEXT, currentSettings));
        }
        return inventory;
    }

    private ItemStack playerItem(PlayerEntry entry, SkinTexture texture, MenuSettings currentSettings) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        Placeholders placeholders = new Placeholders().set("player", entry.name());
        meta.displayName(currentSettings.text(currentSettings.playerItem().name(), placeholders));
        meta.lore(currentSettings.lore(currentSettings.playerItem().lore(), placeholders));
        applySkin(meta, entry, texture);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(actionKey, PersistentDataType.STRING, ACTION_PLAYER);
        data.set(targetNameKey, PersistentDataType.STRING, entry.name());
        data.set(targetUuidKey, PersistentDataType.STRING, entry.uuid().toString());
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private void applySkin(SkullMeta meta, PlayerEntry entry, SkinTexture texture) {
        PlayerProfile profile = Bukkit.createPlayerProfile(entry.uuid(), entry.name());
        if (texture != null) {
            try {
                PlayerTextures textures = profile.getTextures();
                PlayerTextures.SkinModel model = texture.slim() ? PlayerTextures.SkinModel.SLIM : PlayerTextures.SkinModel.CLASSIC;
                textures.setSkin(new URI(texture.skinUrl()).toURL(), model);
                profile.setTextures(textures);
            } catch (IllegalArgumentException | MalformedURLException | URISyntaxException ignored) {
            }
        }
        meta.setOwnerProfile(profile);
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

    private void requestTpa(Player viewer, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return;
        }

        MenuSettings currentSettings = settings;
        if (!plugin.getServer().getPluginManager().isPluginEnabled("HuskHomes")) {
            currentSettings.send(viewer, "messages.huskhomes-missing");
            return;
        }

        if (currentSettings.closeAfterRequest()) {
            viewer.closeInventory();
        }

        String command = currentSettings.tpaCommand().replace("{player}", targetName).trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        viewer.performCommand(command);
        currentSettings.send(viewer, "messages.request-sent", new Placeholders().set("player", targetName));
    }

    private Placeholders pagePlaceholders(int page, int totalPages, int count) {
        return new Placeholders()
                .set("page", page + 1)
                .set("pages", totalPages)
                .set("count", count);
    }

}
