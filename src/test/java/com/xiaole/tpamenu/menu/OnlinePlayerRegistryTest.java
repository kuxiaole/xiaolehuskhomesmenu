package com.xiaole.tpamenu.menu;

import com.xiaole.tpamenu.player.GlobalPlayerProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlinePlayerRegistryTest {
    @Test
    void usesGlobalPlayersAndPreservesTheirServerNames() {
        UUID local = UUID.randomUUID();
        UUID remote = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        GlobalPlayerProvider provider = new TestProvider(
                List.of(
                        new PlayerEntry(remote, "Remote", "skyblock"),
                        new PlayerEntry(local, "Local", "survival")
                ),
                "survival"
        );

        List<PlayerEntry> entries = new OnlinePlayerRegistry(provider).snapshot(viewer, true);

        assertEquals(List.of("Local", "Remote"), entries.stream().map(PlayerEntry::name).toList());
        assertEquals("skyblock", entries.get(1).server());
    }

    @Test
    void canHideViewerFromGlobalSnapshot() {
        UUID viewer = UUID.randomUUID();
        GlobalPlayerProvider provider = new TestProvider(
                List.of(new PlayerEntry(viewer, "Viewer", "lobby")),
                "lobby"
        );

        List<PlayerEntry> entries = new OnlinePlayerRegistry(provider).snapshot(viewer, false);

        assertTrue(entries.isEmpty());
    }

    private record TestProvider(List<PlayerEntry> entries, String localServerName) implements GlobalPlayerProvider {
        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public List<PlayerEntry> snapshot() {
            return entries;
        }
    }
}
