package com.waveai.worker.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
public class CacheSyncService {

    private static final Logger log = LoggerFactory.getLogger(CacheSyncService.class);

    private final WaveAiService waveAiService;
    private final SessionRepository sessionRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final FolderRepository folderRepository;
    private final SseEmitterRegistry sseRegistry;

    public CacheSyncService(
            WaveAiService waveAiService,
            SessionRepository sessionRepository,
            SessionDetailRepository sessionDetailRepository,
            TranscriptSegmentRepository transcriptSegmentRepository,
            FolderRepository folderRepository,
            SseEmitterRegistry sseRegistry) {
        this.waveAiService = waveAiService;
        this.sessionRepository = sessionRepository;
        this.sessionDetailRepository = sessionDetailRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.folderRepository = folderRepository;
        this.sseRegistry = sseRegistry;
    }

    @Scheduled(cron = "${sync.schedule}")
    public void scheduledSync() {
        log.info("Running scheduled sync");
        syncSessions();
        syncFolders();
    }

    public void triggerSessionsSync() {
        CompletableFuture.runAsync(this::syncSessions);
    }

    public void triggerSessionDetailSync(String id) {
        CompletableFuture.runAsync(() -> {
            syncSingleSessionDetail(id);
            syncTranscript(id);
        });
    }

    public void triggerFoldersSync() {
        CompletableFuture.runAsync(this::syncFolders);
    }

    @Transactional
    public void syncSessions() {
        try {
            List<Session> fresh = waveAiService.getSessions();
            List<SessionEntity> entities = fresh.stream().map(this::toSessionEntity).toList();
            sessionRepository.saveAll(entities);
            sseRegistry.broadcast("sessions-updated", "{}");
            log.debug("Synced {} sessions", entities.size());
        } catch (Exception e) {
            log.error("Failed to sync sessions", e);
        }
    }

    @Transactional
    public void syncFolders() {
        try {
            List<Folder> fresh = waveAiService.getFolders();
            List<FolderEntity> entities = fresh.stream().map(this::toFolderEntity).toList();
            folderRepository.saveAll(entities);
            sseRegistry.broadcast("folders-updated", "{}");
            log.debug("Synced {} folders", entities.size());
        } catch (Exception e) {
            log.error("Failed to sync folders", e);
        }
    }

    @Transactional
    public void syncSingleSessionDetail(String id) {
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
            SessionDetailEntity entity = toDetailEntity(detail, session);
            sessionDetailRepository.save(entity);
            sseRegistry.broadcast("session-detail-updated", id);
            log.debug("Synced detail for session {}", id);
        } catch (Exception e) {
            log.error("Failed to sync session detail for {}", id, e);
        }
    }

    @Transactional
    public void syncTranscript(String id) {
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
        } catch (Exception e) {
            log.error("Failed to sync transcript for {}", id, e);
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

    private SessionDetailEntity toDetailEntity(SessionDetail d, SessionEntity session) {
        SessionDetailEntity e = new SessionDetailEntity();
        e.setSession(session);
        e.setLanguage(d.language());
        e.setSummary(d.summary());
        e.setNotes(d.notes());
        e.setSpeakers(d.speakers() != null ? d.speakers() : List.of());
        e.setSessionUrl(d.sessionUrl());
        e.setCachedAt(Instant.now());
        return e;
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
