package com.waveai.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "sync")
public record SyncProperties(
        String schedule,
        Duration rateLimitCooldown,
        Ttl ttl
) {
    public record Ttl(
            Duration sessions,
            Duration folders,
            Duration detail
    ) {}
}
