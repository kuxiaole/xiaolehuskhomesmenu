package com.xiaole.tpamenu.skin;

import com.xiaole.tpamenu.menu.PlayerEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SkinProvider extends AutoCloseable {
    CompletableFuture<Map<UUID, SkinTexture>> loadTextures(Collection<PlayerEntry> players, Duration timeout);

    @Override
    void close();
}
