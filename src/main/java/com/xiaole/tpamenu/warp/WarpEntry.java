package com.xiaole.tpamenu.warp;

import java.util.Objects;

public record WarpEntry(String name, String safeIdentifier, String description) {
    public WarpEntry {
        name = Objects.requireNonNull(name, "name");
        safeIdentifier = Objects.requireNonNull(safeIdentifier, "safeIdentifier");
        description = Objects.requireNonNullElse(description, "");
    }
}
