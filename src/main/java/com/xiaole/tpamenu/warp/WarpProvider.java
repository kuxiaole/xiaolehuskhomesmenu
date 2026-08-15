package com.xiaole.tpamenu.warp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WarpProvider {
    boolean available();

    CompletableFuture<List<WarpEntry>> loadWarps();
}
