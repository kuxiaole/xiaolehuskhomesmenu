package com.xiaole.tpamenu.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class SchedulerUtil {
    private static final boolean FOLIA = detectFolia();

    private final Plugin plugin;

    public SchedulerUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isFolia() {
        return FOLIA;
    }

    public void runAtEntity(Entity entity, Runnable task) {
        if (FOLIA) {
            if (Bukkit.isOwnedByCurrentRegion(entity)) {
                task.run();
                return;
            }
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runAtEntityDelayed(Entity entity, Runnable task, long ticks) {
        long delay = Math.max(1, ticks);
        if (FOLIA) {
            entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delay);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    public void runGlobal(Runnable task) {
        if (FOLIA) {
            if (Bukkit.isGlobalTickThread()) {
                task.run();
                return;
            }
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
