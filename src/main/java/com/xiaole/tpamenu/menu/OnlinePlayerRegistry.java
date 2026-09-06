package com.xiaole.tpamenu.menu;

import com.xiaole.tpamenu.player.GlobalPlayerProvider;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OnlinePlayerRegistry {
    private final ConcurrentHashMap<UUID, String> players = new ConcurrentHashMap<>();
    private final GlobalPlayerProvider globalPlayerProvider;

    public OnlinePlayerRegistry(GlobalPlayerProvider globalPlayerProvider) {
        this.globalPlayerProvider = globalPlayerProvider;
    }

    public void track(Player player) {
        players.put(player.getUniqueId(), player.getName());
    }

    public void untrack(UUID playerId) {
        players.remove(playerId);
    }

    public List<PlayerEntry> snapshot(UUID viewerId, boolean showSelf) {
        Map<UUID, PlayerEntry> merged = new HashMap<>();
        if (globalPlayerProvider != null && globalPlayerProvider.enabled()) {
            for (PlayerEntry entry : globalPlayerProvider.snapshot()) {
                merged.put(entry.uuid(), entry);
            }
        }
        String localServerName = globalPlayerProvider == null ? "本服" : globalPlayerProvider.localServerName();
        players.forEach((uuid, name) -> merged.put(uuid, new PlayerEntry(uuid, name, localServerName)));

        return merged.values().stream()
                .filter(entry -> showSelf || !entry.uuid().equals(viewerId))
                .sorted(Comparator.comparing(PlayerEntry::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PlayerEntry::server, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.uuid().toString()))
                .toList();
    }

    public void clear() {
        players.clear();
    }
}
