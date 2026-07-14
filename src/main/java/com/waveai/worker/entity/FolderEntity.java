package com.waveai.worker.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "folders")
public class FolderEntity {

    @Id
    private String id;
    private String name;
    private String color;
    private int sessionCount;
    private Instant cachedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }

    public Instant getCachedAt() { return cachedAt; }
    public void setCachedAt(Instant cachedAt) { this.cachedAt = cachedAt; }
}
