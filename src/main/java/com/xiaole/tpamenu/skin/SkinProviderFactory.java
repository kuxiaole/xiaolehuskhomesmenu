package com.xiaole.tpamenu.skin;

import com.xiaole.tpamenu.config.MenuSettings;
import org.bukkit.plugin.Plugin;

public final class SkinProviderFactory {
    private SkinProviderFactory() {
    }

    public static SkinProvider create(Plugin plugin, MenuSettings settings) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("SkinsRestorer")) {
            plugin.getLogger().info("SkinsRestorer not found. Player heads will use server profiles as fallback.");
            return new FallbackSkinProvider();
        }

        try {
            return new SkinsRestorerSkinProvider(plugin, settings);
        } catch (LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("Could not hook SkinsRestorer: " + exception.getMessage());
            return new FallbackSkinProvider();
        }
    }
}
