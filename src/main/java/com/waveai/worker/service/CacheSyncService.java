package com.waveai.worker.service;

import com.waveai.worker.config.SyncProperties;
import com.waveai.worker.entity.FolderEntity;
import com.waveai.worker.entity.SessionDetailEntity;
import com.waveai.worker.entity.SessionEntity;
import com.waveai.worker.entity.TranscriptSegmentEntity;
import com.waveai.worker.model.Folder;
import com.waveai.worker.model.Session;
import com.waveai.worker.model.SessionDetail;
import com.waveai.worker.model.TranscriptSegment;
import com.waveai.worker.repository.FolderRepository;
import com.waveai.worker.repository.SessionDetailRepository;
import com.waveai.worker.repository.SessionRepository;
import com.waveai.worker.repository.TranscriptSegmentRepository;
import com.waveai.worker.sse.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Service
public class CacheSyncService {

    private static final Logger log = LoggerFactory.getLogger(CacheSyncService.class);

    private static final String KEY_SESSIONS = "sessions";
    private static final String KEY_FOLDERS = "folders";
    private static final String KEY_DETAIL_PREFIX = "detail:";

    public enum SyncResult { COMPLETED, ALREADY_IN_PROGRESS }

    private final WaveAiService waveAiService;
    private final SessionRepository sessionRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final FolderRepository folderRepository;
    private final SseEmitterRegistry sseRegistry;
    private final SyncProperties syncProperties;
    private final UpstreamRateLimiter rateLimiter;
    private final ThreadPoolTaskExecutor syncExecutor;

    /** Resources with a sync currently running; collapses concurrent triggers into one upstream call. */
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public CacheSyncService(
            WaveAiService waveAiService,
            SessionRepository sessionRepository,
            SessionDetailRepository sessionDetailRepository,
            TranscriptSegmentRepository transcriptSegmentRepository,
            FolderRepository folderRepository,
            SseEmitterRegistry sseRegistry,
            SyncProperties syncProperties,
            UpstreamRateLimiter rateLimiter,
            ThreadPoolTaskExecutor syncExecutor) {
        this.waveAiService = waveAiService;
        this.sessionRepository = sessionRepository;
        this.sessionDetailRepository = sessionDetailRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.folderRepository = folderRepository;
        this.sseRegistry = sseRegistry;
        this.syncProperties = syncProperties;
        this.rateLimiter = rateLimiter;
        this.syncExecutor = syncExecutor;
    }

    @Scheduled(cron = "${sync.schedule}")
    public void scheduledSync() {
        log.info("Running scheduled sync");
        triggerSessionsSync();
        triggerFoldersSync();
    }

    // ---- Background triggers: TTL-gated, rate-limit-gated, deduplicated ----

    public void triggerSessionsSync() {
        if (rateLimiter.isPaused() || sessionsFresh()) return;
        submit(KEY_SESSIONS, () -> {
            if (sessionsFresh()) return;
            doSyncSessions();
        });
    }

    public void triggerFoldersSync() {
        if (rateLimiter.isPaused() || foldersFresh()) return;
        submit(KEY_FOLDERS, () -> {
            if (foldersFresh()) return;
            doSyncFolders();
        });
    }

    public void triggerSessionDetailSync(String id) {
        if (rateLimiter.isPaused() || (detailFresh(id) && transcriptExists(id))) return;
        submit(KEY_DETAIL_PREFIX + id, () -> {
            boolean fresh = detailFresh(id);
            if (fresh && transcriptExists(id)) return;
            if (!fresh) doSyncSessionDetail(id);
            if (!rateLimiter.isPaused()) doSyncTranscript(id);
        });
    }

    /**
     * Dedupe is acquired inside the task (not at submission) so a task discarded
     * by the bounded executor can never leak an in-flight key.
     */
    private void submit(String key, Runnable work) {
        syncExecutor.execute(() -> {
            if (!inFlight.add(key)) return;
            try {
                work.run();
            } catch (RateLimitedException e) {
                log.warn("Sync of {} paused by WaveAI rate limit for {}", key, rateLimiter.remaining());
            } catch (Exception e) {
                log.error("Background sync of {} failed", key, e);
            } finally {
                inFlight.remove(key);
            }
        });
    }

    // ---- Manual (on-demand) syncs: bypass TTL, respect rate limiter + dedupe ----

    public SyncResult syncSessionsNow() {
        if (!inFlight.add(KEY_SESSIONS)) return SyncResult.ALREADY_IN_PROGRESS;
        try {
            doSyncSessions();
            return SyncResult.COMPLETED;
        } finally {
            inFlight.remove(KEY_SESSIONS);
        }
    }

    public SyncResult syncFoldersNow() {
        if (!inFlight.add(KEY_FOLDERS)) return SyncResult.ALREADY_IN_PROGRESS;
        try {
            doSyncFolders();
            return SyncResult.COMPLETED;
        } finally {
            inFlight.remove(KEY_FOLDERS);
        }
    }

    public SyncResult syncSessionDetailNow(String id) {
        String key = KEY_DETAIL_PREFIX + id;
        if (!inFlight.add(key)) return SyncResult.ALREADY_IN_PROGRESS;
        try {
            doSyncSessionDetail(id);
            doSyncTranscript(id);
            return SyncResult.COMPLETED;
        } finally {
            inFlight.remove(key);
        }
    }

    // ---- Freshness checks ----

    private boolean sessionsFresh() {
        return isFresh(sessionRepository.findMaxCachedAt(), syncProperties.ttl().sessions());
    }

    private boolean foldersFresh() {
        return isFresh(folderRepository.findMaxCachedAt(), syncProperties.ttl().folders());
    }

    private boolean detailFresh(String id) {
        return isFresh(sessionDetailRepository.findById(id).map(SessionDetailEntity::getCachedAt),
                syncProperties.ttl().detail());
    }

    private boolean transcriptExists(String id) {
        return transcriptSegmentRepository.existsBySessionId(id);
    }

    private static boolean isFresh(Optional<Instant> cachedAt, Duration ttl) {
        return cachedAt
                .map(t -> t.plus(ttl).isAfter(Instant.now()))
                .orElse(false);
    }

    // ---- Sync bodies: throw on failure; 429 is recorded then rethrown ----

    private void doSyncSessions() {
        try {
            List<Session> fresh = waveAiService.getSessions();
            List<SessionEntity> entities = fresh.stream().map(this::toSessionEntity).toList();
            sessionRepository.saveAll(entities);
            sseRegistry.broadcast("sessions-updated", "{}");
            log.debug("Synced {} sessions", entities.size());
        } catch (RateLimitedException e) {
            rateLimiter.recordRateLimit(e.retryAfter());
            throw e;
        }
    }

    private void doSyncFolders() {
        try {
            List<Folder> fresh = waveAiService.getFolders();
            List<FolderEntity> entities = fresh.stream().map(this::toFolderEntity).toList();
            folderRepository.saveAll(entities);
            sseRegistry.broadcast("folders-updated", "{}");
            log.debug("Synced {} folders", entities.size());
        } catch (RateLimitedException e) {
            rateLimiter.recordRateLimit(e.retryAfter());
            throw e;
        }
    }

    private void doSyncSessionDetail(String id) {
        try {
            SessionDetail detail = waveAiService.getSessionDetail(id);
            if (detail == null) return;
            SessionEntity session = sessionRepository.findById(id).orElseGet(() -> {
                SessionEntity s = new SessionEntity();
                s.setId(id);
                s.setTitle(detail.title());
                s.setTimestamp(detail.timestamp());
                s.setDurationSeconds(detail.durationSeconds());
                s.setType(detail.type());
                s.setPlatform(detail.platform());
                s.setCachedAt(Instant.now());
                return sessionRepository.save(s);
            });
            SessionDetailEntity entity = sessionDetailRepository.findById(id).orElseGet(() -> {
                SessionDetailEntity e = new SessionDetailEntity();
                e.setSession(session);
                return e;
            });
            applyDetail(entity, detail);
            sessionDetailRepository.save(entity);
            sseRegistry.broadcast("session-detail-updated", id);
            log.debug("Synced detail for session {}", id);
        } catch (RateLimitedException e) {
            rateLimiter.recordRateLimit(e.retryAfter());
            throw e;
        }
    }

    private void doSyncTranscript(String id) {
        try {
            SessionEntity session = sessionRepository.findById(id).orElse(null);
            if (session == null) return;
            List<TranscriptSegment> segments = waveAiService.getTranscript(id);
            transcriptSegmentRepository.deleteBySessionId(id);
            List<TranscriptSegmentEntity> entities = IntStream.range(0, segments.size())
                .mapToObj(i -> toSegmentEntity(session, segments.get(i), i))
                .toList();
            transcriptSegmentRepository.saveAll(entities);
            sseRegistry.broadcast("transcript-updated", id);
            log.debug("Synced {} transcript segments for session {}", entities.size(), id);
        } catch (RateLimitedException e) {
            rateLimiter.recordRateLimit(e.retryAfter());
            throw e;
        }
    }

    private SessionEntity toSessionEntity(Session s) {
        SessionEntity e = new SessionEntity();
        e.setId(s.id());
        e.setTitle(s.title());
        e.setTimestamp(s.timestamp());
        e.setDurationSeconds(s.durationSeconds());
        e.setType(s.type());
        e.setPlatform(s.platform());
        e.setCachedAt(Instant.now());
        return e;
    }

    private void applyDetail(SessionDetailEntity e, SessionDetail d) {
        e.setLanguage(d.language());
        e.setSummary(d.summary());
        e.setNotes(d.notes());
        e.setSpeakers(d.speakers() != null ? d.speakers() : List.of());
        e.setSessionUrl(d.sessionUrl());
        e.setCachedAt(Instant.now());
    }

    private FolderEntity toFolderEntity(Folder f) {
        FolderEntity e = new FolderEntity();
        e.setId(f.id());
        e.setName(f.name());
        e.setColor(f.color());
        e.setSessionCount(f.sessionCount());
        e.setCachedAt(Instant.now());
        return e;
    }

    private TranscriptSegmentEntity toSegmentEntity(SessionEntity session, TranscriptSegment s, int idx) {
        TranscriptSegmentEntity e = new TranscriptSegmentEntity();
        e.setSession(session);
        e.setSpeaker(s.speaker());
        e.setStartSec(s.start());
        e.setEndSec(s.end());
        e.setText(s.text());
        e.setSegmentIdx(idx);
        return e;
    }
}
