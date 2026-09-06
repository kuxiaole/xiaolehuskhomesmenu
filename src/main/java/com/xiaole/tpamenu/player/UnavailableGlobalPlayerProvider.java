package com.xiaole.tpamenu.player;

import com.xiaole.tpamenu.menu.PlayerEntry;

import java.util.List;

final class UnavailableGlobalPlayerProvider implements GlobalPlayerProvider {
    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public String localServerName() {
        return "本服";
    }

    @Override
    public List<PlayerEntry> snapshot() {
        return List.of();
    }
}
