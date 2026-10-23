package com.argorice.epicysm.client;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderHistoryTest {
    private final UUID player = UUID.randomUUID();
    private final Object world = new Object();
    private final Object entity = new Object();
    private final RenderHistory<Object> history = new RenderHistory<>(10);

    @Test
    void respawnDoesNotInheritRenderOwnership() {
        this.history.record(this.player, this.entity, this.world, 1350);
        Object respawned = new Object();

        assertFalse(this.history.contains(this.player, respawned, this.world, 0));
        assertFalse(this.history.contains(this.player, respawned, this.world, 1350));
        this.history.record(this.player, respawned, this.world, 1);
        assertTrue(this.history.contains(this.player, respawned, this.world, 1));
        assertFalse(this.history.contains(this.player, this.entity, this.world, 1351));
    }

    @Test
    void worldReplacementInvalidatesTheSameEntity() {
        this.history.record(this.player, this.entity, this.world, 100);

        assertFalse(this.history.contains(this.player, this.entity, new Object(), 100));
    }

    @Test
    void clockRollbackCannotKeepAnOldRenderAlive() {
        this.history.record(this.player, this.entity, this.world, 1350);

        assertFalse(this.history.contains(this.player, this.entity, this.world, 0));
        assertFalse(this.history.contains(this.player, this.entity, this.world, 1349));
    }

    @Test
    void ownershipExpiresAfterTenTicks() {
        this.history.record(this.player, this.entity, this.world, 100);

        assertTrue(this.history.contains(this.player, this.entity, this.world, 100));
        assertTrue(this.history.contains(this.player, this.entity, this.world, 110));
        assertFalse(this.history.contains(this.player, this.entity, this.world, 111));
    }

    @Test
    void tickOverflowDoesNotReviveOldOwnership() {
        this.history.record(this.player, this.entity, this.world, Integer.MAX_VALUE);

        assertFalse(this.history.contains(this.player, this.entity, this.world, Integer.MIN_VALUE));
    }

    @Test
    void reloadAndPlayerRemovalDiscardRenderOwnership() {
        this.history.record(this.player, this.entity, this.world, 100);
        this.history.remove(this.player);
        assertFalse(this.history.contains(this.player, this.entity, this.world, 100));

        this.history.record(this.player, this.entity, this.world, 100);
        this.history.clear();
        assertFalse(this.history.contains(this.player, this.entity, this.world, 100));
    }

    @Test
    void cleanupRetainsOnlyPresentPlayers() {
        UUID departed = UUID.randomUUID();
        this.history.record(this.player, this.entity, this.world, 100);
        this.history.record(departed, this.entity, this.world, 100);
        this.history.retainAll(Set.of(this.player));

        assertTrue(this.history.contains(this.player, this.entity, this.world, 100));
        assertFalse(this.history.contains(departed, this.entity, this.world, 100));
    }
}
