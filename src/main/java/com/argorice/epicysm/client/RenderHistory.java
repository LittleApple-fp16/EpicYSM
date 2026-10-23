package com.argorice.epicysm.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Render ownership recorded for one entity instance in one world. */
final class RenderHistory<T> {
    private final int lifetime;
    private final Map<UUID, Entry<T>> entries = new HashMap<>();

    RenderHistory(int lifetime) {
        if (lifetime < 0) {
            throw new IllegalArgumentException("Render history lifetime must be nonnegative");
        }

        this.lifetime = lifetime;
    }

    void record(UUID id, T entity, Object world, int tick) {
        this.entries.put(id, new Entry<>(entity, world, tick));
    }

    boolean contains(UUID id, T entity, Object world, int tick) {
        Entry<T> entry = this.entries.get(id);

        if (entry == null || entry.entity() != entity || entry.world() != world) {
            return false;
        }

        long age = (long) tick - entry.tick();
        return age >= 0 && age <= this.lifetime;
    }

    void remove(UUID id) {
        this.entries.remove(id);
    }

    void retainAll(Set<UUID> present) {
        this.entries.keySet().retainAll(present);
    }

    void clear() {
        this.entries.clear();
    }

    private record Entry<T>(T entity, Object world, int tick) {
    }
}
