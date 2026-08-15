package com.xiaole.tpamenu.menu;

import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OnlinePlayerRegistry {
    private final ConcurrentHashMap<UUID, String> players = new ConcurrentHashMap<>();

    public void track(Player player) {
        players.put(player.getUniqueId(), player.getName());
    }

    public void untrack(UUID playerId) {
        players.remove(playerId);
    }

    public List<PlayerEntry> snapshot(UUID viewerId, boolean showSelf) {
        return players.entrySet().stream()
                .filter(entry -> showSelf || !entry.getKey().equals(viewerId))
                .map(entry -> new PlayerEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PlayerEntry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void clear() {
        players.clear();
    }
}
