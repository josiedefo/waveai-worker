package com.waveai.worker.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    private String id;
    private String title;
    private Instant timestamp;
    private long durationSeconds;
    private String type;
    private String platform;
    private Instant cachedAt;

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SessionDetailEntity detail;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Instant getCachedAt() { return cachedAt; }
    public void setCachedAt(Instant cachedAt) { this.cachedAt = cachedAt; }

    public SessionDetailEntity getDetail() { return detail; }
    public void setDetail(SessionDetailEntity detail) { this.detail = detail; }
}
