package com.xiaole.tpamenu.skin;

import com.xiaole.tpamenu.menu.PlayerEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class FallbackSkinProvider implements SkinProvider {
    @Override
    public CompletableFuture<Map<UUID, SkinTexture>> loadTextures(Collection<PlayerEntry> players, Duration timeout) {
        return CompletableFuture.completedFuture(Collections.emptyMap());
    }

    @Override
    public void close() {
    }
}
