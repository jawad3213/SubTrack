package com.subtrack.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window counter used to slow down password guessing and reset-email abuse.
 * Keys are free-form (e.g. "login:email:x@y.com", "login:ip:1.2.3.4"). State is per server
 * instance and is lost on restart, which is acceptable for throttling.
 */
@ApplicationScoped
public class AttemptLimiter {

    /** Old windows are swept after this many recordings, so unused keys don't accumulate. */
    private static final int SWEEP_EVERY = 1000;
    private static final Duration MAX_WINDOW = Duration.ofHours(24);

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private int recordsSinceSweep;

    public AttemptLimiter() {
        this(Clock.systemUTC());
    }

    AttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** True if {@code key} already has {@code max} or more attempts within {@code window}. */
    public boolean isBlocked(String key, int max, Duration window) {
        Deque<Instant> times = attempts.get(key);
        if (times == null) {
            return false;
        }
        synchronized (times) {
            prune(times, window);
            return times.size() >= max;
        }
    }

    public void record(String key) {
        Deque<Instant> times = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            times.addLast(clock.instant());
        }
        sweepOccasionally();
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void prune(Deque<Instant> times, Duration window) {
        Instant cutoff = clock.instant().minus(window);
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.removeFirst();
        }
    }

    private synchronized void sweepOccasionally() {
        if (++recordsSinceSweep < SWEEP_EVERY) {
            return;
        }
        recordsSinceSweep = 0;
        attempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), MAX_WINDOW);
                return entry.getValue().isEmpty();
            }
        });
    }
}
