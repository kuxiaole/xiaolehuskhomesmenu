package com.xiaole.tpamenu.player;

import com.xiaole.tpamenu.menu.PlayerEntry;
import net.william278.huskhomes.BukkitHuskHomes;
import net.william278.huskhomes.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class HuskHomesGlobalPlayerProvider implements GlobalPlayerProvider {
    private final BukkitHuskHomes huskHomes;
    private final String localServerName;
    private final boolean enabled;

    HuskHomesGlobalPlayerProvider(BukkitHuskHomes huskHomes) {
        this.huskHomes = huskHomes;
        this.localServerName = readLocalServerName(huskHomes);
        this.enabled = readCrossServerEnabled(huskHomes);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public String localServerName() {
        return localServerName;
    }

    @Override
    public List<PlayerEntry> snapshot() {
        Map<UUID, PlayerEntry> result = new HashMap<>();
        Map<String, List<User>> usersByServer;
        try {
            usersByServer = huskHomes.getGlobalUserList();
        } catch (LinkageError | RuntimeException ignored) {
            return List.of();
        }
        if (usersByServer == null || usersByServer.isEmpty()) {
            return List.of();
        }

        usersByServer.forEach((server, users) -> {
            if (server == null || server.isBlank() || users == null) {
                return;
            }
            for (User user : safeCopy(users)) {
                if (user == null || user.getUuid() == null || user.getName() == null || user.getName().isBlank()) {
                    continue;
                }
                result.put(user.getUuid(), new PlayerEntry(user.getUuid(), user.getName(), server));
            }
        });
        return List.copyOf(result.values());
    }

    private static List<User> safeCopy(List<User> users) {
        try {
            return new ArrayList<>(users);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static String readLocalServerName(BukkitHuskHomes huskHomes) {
        try {
            String name = huskHomes.getServerName();
            return name == null || name.isBlank() ? "本服" : name;
        } catch (LinkageError | RuntimeException ignored) {
            return "本服";
        }
    }

    private static boolean readCrossServerEnabled(BukkitHuskHomes huskHomes) {
        try {
            return huskHomes.getSettings() != null
                    && huskHomes.getSettings().getCrossServer() != null
                    && huskHomes.getSettings().getCrossServer().isEnabled();
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
