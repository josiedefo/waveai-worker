package com.waveai.worker.service;

import java.time.Duration;

/**
 * Thrown when the upstream WaveAI API responds with HTTP 429.
 * {@code retryAfter} is the parsed Retry-After header, or null if absent.
 */
public class RateLimitedException extends RuntimeException {

    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter, Throwable cause) {
        super("WaveAI API rate limit reached", cause);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
