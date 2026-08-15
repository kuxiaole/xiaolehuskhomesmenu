package com.xiaole.tpamenu.warp;

import org.bukkit.plugin.Plugin;

public final class WarpProviderFactory {
    private WarpProviderFactory() {
    }

    public static WarpProvider create(Plugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("HuskHomes")) {
            plugin.getLogger().warning("HuskHomes not found. The warp menu will remain unavailable.");
            return new UnavailableWarpProvider();
        }

        try {
            WarpProvider provider = new HuskHomesWarpProvider();
            plugin.getLogger().info("Hooked HuskHomes 4.10 warp API.");
            return provider;
        } catch (LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("Could not hook HuskHomes warp API: " + exception.getMessage());
            return new UnavailableWarpProvider();
        }
    }
}
