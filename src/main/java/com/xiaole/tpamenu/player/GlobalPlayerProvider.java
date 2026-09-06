package com.xiaole.tpamenu.player;

import com.xiaole.tpamenu.menu.PlayerEntry;

import java.util.List;

public interface GlobalPlayerProvider {
    boolean enabled();

    String localServerName();

    List<PlayerEntry> snapshot();
}
