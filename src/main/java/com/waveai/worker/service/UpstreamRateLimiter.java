package com.waveai.worker.service;

import com.waveai.worker.config.SyncProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global cooldown shared by every upstream sync path. When WaveAI returns 429,
 * all sync attempts (background and manual) pause until the cooldown expires.
 */
@Component
public class UpstreamRateLimiter {

    private final AtomicReference<Instant> pausedUntil = new AtomicReference<>(Instant.EPOCH);
    private final Duration fallbackCooldown;

    public UpstreamRateLimiter(SyncProperties properties) {
        this.fallbackCooldown = properties.rateLimitCooldown() != null
                ? properties.rateLimitCooldown()
                : Duration.ofSeconds(60);
    }

    public boolean isPaused() {
        return Instant.now().isBefore(pausedUntil.get());
    }

    public Duration remaining() {
        Duration remaining = Duration.between(Instant.now(), pausedUntil.get());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public void recordRateLimit(Duration retryAfter) {
        Duration cooldown = retryAfter != null && retryAfter.compareTo(fallbackCooldown) > 0
                ? retryAfter
                : fallbackCooldown;
        Instant until = Instant.now().plus(cooldown);
        // Pauses only extend, never shorten.
        pausedUntil.accumulateAndGet(until, (current, next) -> current.isAfter(next) ? current : next);
    }
}
