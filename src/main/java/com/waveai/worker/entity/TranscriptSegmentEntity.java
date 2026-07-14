package com.waveai.worker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transcript_segments")
public class TranscriptSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    private String speaker;
    private Double startSec;
    private Double endSec;

    @Column(nullable = false)
    private String text;

    private int segmentIdx;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }

    public Double getStartSec() { return startSec; }
    public void setStartSec(Double startSec) { this.startSec = startSec; }

    public Double getEndSec() { return endSec; }
    public void setEndSec(Double endSec) { this.endSec = endSec; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getSegmentIdx() { return segmentIdx; }
    public void setSegmentIdx(int segmentIdx) { this.segmentIdx = segmentIdx; }
}
