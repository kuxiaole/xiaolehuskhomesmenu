package com.xiaole.tpamenu;

import com.xiaole.tpamenu.command.TpaMenuCommand;
import com.xiaole.tpamenu.command.WarpMenuCommand;
import com.xiaole.tpamenu.config.MenuSettings;
import com.xiaole.tpamenu.listener.TpaMenuListener;
import com.xiaole.tpamenu.listener.WarpMenuListener;
import com.xiaole.tpamenu.menu.OnlinePlayerRegistry;
import com.xiaole.tpamenu.menu.TpaMenuService;
import com.xiaole.tpamenu.menu.WarpMenuService;
import com.xiaole.tpamenu.scheduler.SchedulerUtil;
import com.xiaole.tpamenu.skin.SkinProvider;
import com.xiaole.tpamenu.skin.SkinProviderFactory;
import com.xiaole.tpamenu.warp.WarpProvider;
import com.xiaole.tpamenu.warp.WarpProviderFactory;
import org.bukkit.plugin.java.JavaPlugin;

public final class TpaMenuPlugin extends JavaPlugin {
    private SchedulerUtil scheduler;
    private MenuSettings settings;
    private SkinProvider skinProvider;
    private OnlinePlayerRegistry onlinePlayers;
    private TpaMenuService menuService;
    private WarpMenuService warpMenuService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        scheduler = new SchedulerUtil(this);
        reloadLocalSettings();

        skinProvider = SkinProviderFactory.create(this, settings);
        onlinePlayers = new OnlinePlayerRegistry();
        menuService = new TpaMenuService(this, scheduler, onlinePlayers, skinProvider, settings);
        WarpProvider warpProvider = WarpProviderFactory.create(this);
        warpMenuService = new WarpMenuService(this, scheduler, warpProvider, settings);

        TpaMenuCommand command = new TpaMenuCommand(this, menuService);
        if (getCommand("tpamenu") != null) {
            getCommand("tpamenu").setExecutor(command);
            getCommand("tpamenu").setTabCompleter(command);
        }
        WarpMenuCommand warpCommand = new WarpMenuCommand(this, warpMenuService);
        if (getCommand("warpmenu") != null) {
            getCommand("warpmenu").setExecutor(warpCommand);
            getCommand("warpmenu").setTabCompleter(warpCommand);
        }

        getServer().getPluginManager().registerEvents(new TpaMenuListener(menuService, onlinePlayers), this);
        getServer().getPluginManager().registerEvents(new WarpMenuListener(warpMenuService), this);
        seedOnlinePlayers();
        getLogger().info("xiaolehuskhomesmenu enabled. Folia detected: " + scheduler.isFolia());
    }

    @Override
    public void onDisable() {
        if (menuService != null) {
            menuService.shutdown();
        }
        if (warpMenuService != null) {
            warpMenuService.shutdown();
        }
        if (onlinePlayers != null) {
            onlinePlayers.clear();
        }
        if (skinProvider != null) {
            skinProvider.close();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadLocalSettings();
        SkinProvider previousProvider = skinProvider;
        SkinProvider replacementProvider = SkinProviderFactory.create(this, settings);
        skinProvider = replacementProvider;
        menuService.update(settings, replacementProvider);
        warpMenuService.update(settings);
        if (previousProvider != null) {
            previousProvider.close();
        }
    }

    public MenuSettings settings() {
        return settings;
    }

    private void reloadLocalSettings() {
        settings = MenuSettings.from(getConfig());
    }

    private void seedOnlinePlayers() {
        scheduler.runGlobal(() -> getServer().getOnlinePlayers().forEach(player ->
                scheduler.runAtEntity(player, () -> onlinePlayers.track(player))
        ));
    }
}
