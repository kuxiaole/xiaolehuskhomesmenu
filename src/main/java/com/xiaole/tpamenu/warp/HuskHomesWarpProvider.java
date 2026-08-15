package com.xiaole.tpamenu.warp;

import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Warp;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class HuskHomesWarpProvider implements WarpProvider {
    private static final Comparator<WarpEntry> BY_NAME = Comparator
            .comparing(WarpEntry::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(WarpEntry::name);

    private final HuskHomesAPI api;

    HuskHomesWarpProvider() {
        this.api = HuskHomesAPI.getInstance();
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public CompletableFuture<List<WarpEntry>> loadWarps() {
        return api.getWarps().thenApply(warps -> warps.stream()
                .filter(Objects::nonNull)
                .map(HuskHomesWarpProvider::snapshot)
                .sorted(BY_NAME)
                .toList());
    }

    private static WarpEntry snapshot(Warp warp) {
        return new WarpEntry(
                warp.getName(),
                warp.getSafeIdentifier(),
                warp.getMeta().getDescription()
        );
    }
}
