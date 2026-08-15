package com.xiaole.tpamenu.warp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class UnavailableWarpProvider implements WarpProvider {
    @Override
    public boolean available() {
        return false;
    }

    @Override
    public CompletableFuture<List<WarpEntry>> loadWarps() {
        return CompletableFuture.completedFuture(List.of());
    }
}
