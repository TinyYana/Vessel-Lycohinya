package org.maboroshi.vessel.manager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents the same capture/release target from being processed twice concurrently — double
 * interact events, rapid double-clicks, or overlapping scheduled tasks acting on the same entity or
 * the same Vessel ID. Capture/release logic is synchronous within a single event handler call today,
 * so this mainly guards against future scheduler hops reintroducing a window for it; the cost of
 * checking is negligible either way.
 */
public final class InFlightGuard {
    private final Set<String> active = ConcurrentHashMap.newKeySet();

    /** Returns true if {@code key} was not already in-flight (and is now claimed by the caller). */
    public boolean tryAcquire(String key) {
        return key != null && active.add(key);
    }

    public void release(String key) {
        if (key != null) {
            active.remove(key);
        }
    }
}
