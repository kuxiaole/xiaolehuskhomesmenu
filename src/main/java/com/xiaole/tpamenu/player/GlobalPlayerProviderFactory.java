package com.xiaole.tpamenu.player;

import net.william278.huskhomes.BukkitHuskHomes;
import org.bukkit.plugin.Plugin;

public final class GlobalPlayerProviderFactory {
    private GlobalPlayerProviderFactory() {
    }

    public static GlobalPlayerProvider create(Plugin plugin) {
        Plugin candidate = plugin.getServer().getPluginManager().getPlugin("HuskHomes");
        if (candidate == null || !candidate.isEnabled()) {
            return new UnavailableGlobalPlayerProvider();
        }

        try {
            if (!(candidate instanceof BukkitHuskHomes huskHomes)) {
                return new UnavailableGlobalPlayerProvider();
            }
            GlobalPlayerProvider provider = new HuskHomesGlobalPlayerProvider(huskHomes);
            if (provider.enabled()) {
                plugin.getLogger().info("Hooked HuskHomes cross-server online player list.");
            } else {
                plugin.getLogger().info("HuskHomes cross-server mode is disabled; TPA menu will show local players only.");
            }
            return provider;
        } catch (LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("Could not hook HuskHomes cross-server player list: " + exception.getMessage());
            return new UnavailableGlobalPlayerProvider();
        }
    }
}
