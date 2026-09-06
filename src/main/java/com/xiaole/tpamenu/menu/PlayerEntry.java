package com.xiaole.tpamenu.menu;

import java.util.Objects;
import java.util.UUID;

public record PlayerEntry(UUID uuid, String name, String server) {
    public PlayerEntry {
        uuid = Objects.requireNonNull(uuid, "uuid");
        name = Objects.requireNonNull(name, "name");
        server = server == null || server.isBlank() ? "本服" : server;
    }

    public PlayerEntry(UUID uuid, String name) {
        this(uuid, name, "本服");
    }
}
