package com.waveai.worker.service;

import com.waveai.worker.config.SyncProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class UpstreamRateLimiterTest {

    private static SyncProperties props(Duration cooldown) {
        return new SyncProperties(
                "0 0 * * * *",
                cooldown,
                new SyncProperties.Ttl(Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30)));
    }

    @Test
    void notPausedInitially() {
        UpstreamRateLimiter limiter = new UpstreamRateLimiter(props(Duration.ofSeconds(60)));

        assertThat(limiter.isPaused()).isFalse();
        assertThat(limiter.remaining()).isEqualTo(Duration.ZERO);
    }

    @Test
    void recordWithoutRetryAfter_usesFallbackCooldown() {
        UpstreamRateLimiter limiter = new UpstreamRateLimiter(props(Duration.ofSeconds(60)));

        limiter.recordRateLimit(null);

        assertThat(limiter.isPaused()).isTrue();
        assertThat(limiter.remaining()).isLessThanOrEqualTo(Duration.ofSeconds(60));
        assertThat(limiter.remaining()).isGreaterThan(Duration.ofSeconds(55));
    }

    @Test
    void longerRetryAfter_winsOverFallback() {
        UpstreamRateLimiter limiter = new UpstreamRateLimiter(props(Duration.ofSeconds(60)));

        limiter.recordRateLimit(Duration.ofSeconds(300));

        assertThat(limiter.remaining()).isGreaterThan(Duration.ofSeconds(295));
    }

    @Test
    void shorterRetryAfter_doesNotShortenExistingPause() {
        UpstreamRateLimiter limiter = new UpstreamRateLimiter(props(Duration.ofSeconds(10)));

        limiter.recordRateLimit(Duration.ofSeconds(300));
        limiter.recordRateLimit(Duration.ofSeconds(30));

        assertThat(limiter.remaining()).isGreaterThan(Duration.ofSeconds(295));
    }

    @Test
    void nullCooldownProperty_fallsBackToDefault() {
        UpstreamRateLimiter limiter = new UpstreamRateLimiter(props(null));

        limiter.recordRateLimit(null);

        assertThat(limiter.isPaused()).isTrue();
    }
}
