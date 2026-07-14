CREATE TABLE sessions (
    id               VARCHAR(255) PRIMARY KEY,
    title            TEXT,
    timestamp        TIMESTAMPTZ NOT NULL,
    duration_seconds BIGINT NOT NULL DEFAULT 0,
    type             VARCHAR(100),
    platform         VARCHAR(100),
    cached_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE session_details (
    id          VARCHAR(255) PRIMARY KEY REFERENCES sessions(id) ON DELETE CASCADE,
    language    VARCHAR(50),
    summary     TEXT,
    notes       TEXT,
    speakers    JSONB NOT NULL DEFAULT '[]',
    session_url TEXT,
    cached_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transcript_segments (
    id          BIGSERIAL PRIMARY KEY,
    session_id  VARCHAR(255) NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    speaker     TEXT,
    start_sec   DOUBLE PRECISION,
    end_sec     DOUBLE PRECISION,
    text        TEXT NOT NULL,
    segment_idx INT NOT NULL,
    UNIQUE (session_id, segment_idx)
);

CREATE INDEX idx_transcript_session_id ON transcript_segments(session_id);

CREATE TABLE folders (
    id            VARCHAR(255) PRIMARY KEY,
    name          TEXT NOT NULL,
    color         VARCHAR(50),
    session_count INT NOT NULL DEFAULT 0,
    cached_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
