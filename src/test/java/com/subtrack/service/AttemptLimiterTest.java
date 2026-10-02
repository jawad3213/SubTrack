package com.subtrack.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AttemptLimiterTest {

    /** Clock whose time the test can move forward. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final AttemptLimiter limiter = new AttemptLimiter(clock);
    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Test
    void blocksAfterMaxAttempts() {
        for (int i = 0; i < 5; i++) {
            assertFalse(limiter.isBlocked("k", 5, WINDOW));
            limiter.record("k");
        }
        assertTrue(limiter.isBlocked("k", 5, WINDOW));
        assertFalse(limiter.isBlocked("other", 5, WINDOW));
    }

    @Test
    void unblocksWhenAttemptsLeaveTheWindow() {
        for (int i = 0; i < 5; i++) {
            limiter.record("k");
        }
        clock.advance(WINDOW.plusSeconds(1));
        assertFalse(limiter.isBlocked("k", 5, WINDOW));
    }

    @Test
    void resetClearsAttempts() {
        for (int i = 0; i < 5; i++) {
            limiter.record("k");
        }
        limiter.reset("k");
        assertFalse(limiter.isBlocked("k", 5, WINDOW));
    }
}
