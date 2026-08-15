package com.xiaole.tpamenu.skin;

import com.xiaole.tpamenu.config.MenuSettings;
import com.xiaole.tpamenu.menu.PlayerEntry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SkinsRestorerSkinProvider implements SkinProvider {
    private final Plugin plugin;
    private final SkinsRestorer skinsRestorer;
    private final ExecutorService executor;
    private final ConcurrentHashMap<UUID, CachedTexture> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Optional<SkinTexture>>> inFlight = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final long cacheMillis;
    private final boolean onlineMode;

    public SkinsRestorerSkinProvider(Plugin plugin, MenuSettings settings) {
        this.plugin = plugin;
        this.skinsRestorer = SkinsRestorerProvider.get();
        this.cacheMillis = settings.skinCacheTtl().toMillis();
        this.onlineMode = plugin.getServer().getOnlineMode();
        this.executor = Executors.newFixedThreadPool(settings.skinThreads(), new SkinThreadFactory());
        plugin.getLogger().info("Hooked SkinsRestorer for player head textures.");
    }

    @Override
    public CompletableFuture<Map<UUID, SkinTexture>> loadTextures(Collection<PlayerEntry> players, Duration timeout) {
        if (closed.get()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        cleanupExpired();

        ConcurrentHashMap<UUID, SkinTexture> result = new ConcurrentHashMap<>();
        CompletableFuture<?>[] futures = players.stream()
                .map(entry -> loadTexture(entry).thenAccept(texture -> texture.ifPresent(value -> result.put(entry.uuid(), value))))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .completeOnTimeout(null, Math.max(0, timeout.toMillis()), TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> null)
                .thenApply(ignored -> Map.copyOf(result));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        inFlight.values().forEach(future -> future.cancel(true));
        inFlight.clear();
        executor.shutdownNow();
        cache.clear();
    }

    private CompletableFuture<Optional<SkinTexture>> loadTexture(PlayerEntry entry) {
        CachedTexture cached = cache.get(entry.uuid());
        if (cached != null && !cached.expired()) {
            return CompletableFuture.completedFuture(Optional.of(cached.texture()));
        }
        if (closed.get()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final CompletableFuture<Optional<SkinTexture>> future;
        try {
            future = inFlight.computeIfAbsent(
                    entry.uuid(),
                    ignored -> CompletableFuture.supplyAsync(() -> fetch(entry), executor)
            );
        } catch (RejectedExecutionException exception) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        future.whenComplete((ignored, throwable) -> inFlight.remove(entry.uuid(), future));
        return future;
    }

    private Optional<SkinTexture> fetch(PlayerEntry entry) {
        try {
            Optional<SkinProperty> property = skinsRestorer.getPlayerStorage()
                    .getSkinForPlayer(entry.uuid(), entry.name(), onlineMode);
            Optional<SkinTexture> texture = property.flatMap(this::toTexture);
            if (!closed.get()) {
                texture.ifPresent(value -> cache.put(entry.uuid(), new CachedTexture(value, Instant.now().plusMillis(cacheMillis))));
            }
            return texture;
        } catch (DataRequestException exception) {
            plugin.getLogger().fine("Could not load skin for " + entry.name() + ": " + exception.getMessage());
            return Optional.empty();
        } catch (RuntimeException exception) {
            plugin.getLogger().fine("Unexpected SkinsRestorer error for " + entry.name() + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<SkinTexture> toTexture(SkinProperty property) {
        try {
            String json = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject textures = root.getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) {
                return Optional.empty();
            }

            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (!skin.has("url")) {
                return Optional.empty();
            }

            boolean slim = false;
            if (skin.has("metadata")) {
                JsonObject metadata = skin.getAsJsonObject("metadata");
                slim = metadata.has("model") && "slim".equalsIgnoreCase(metadata.get("model").getAsString());
            }
            return Optional.of(new SkinTexture(skin.get("url").getAsString(), slim));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private void cleanupExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().expired());
    }

    private record CachedTexture(SkinTexture texture, Instant expiresAt) {
        private boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private static final class SkinThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "xiaolehuskhomesmenu-Skins-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
