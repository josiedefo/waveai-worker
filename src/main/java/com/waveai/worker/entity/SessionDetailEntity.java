package com.waveai.worker.entity;

import com.waveai.worker.entity.converter.StringListJsonConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "session_details")
public class SessionDetailEntity {

    @Id
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private SessionEntity session;

    private String language;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "JSONB")
    // The converter binds a varchar parameter; Postgres refuses varchar → jsonb
    // without an explicit cast.
    @ColumnTransformer(write = "?::jsonb")
    private List<String> speakers;

    private String sessionUrl;
    private Instant cachedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<String> getSpeakers() { return speakers; }
    public void setSpeakers(List<String> speakers) { this.speakers = speakers; }

    public String getSessionUrl() { return sessionUrl; }
    public void setSessionUrl(String sessionUrl) { this.sessionUrl = sessionUrl; }

    public Instant getCachedAt() { return cachedAt; }
    public void setCachedAt(Instant cachedAt) { this.cachedAt = cachedAt; }
}
