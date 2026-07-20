package com.waveai.worker.controller;

import com.waveai.worker.entity.SessionDetailEntity;
import com.waveai.worker.repository.FolderRepository;
import com.waveai.worker.repository.SessionDetailRepository;
import com.waveai.worker.repository.SessionRepository;
import com.waveai.worker.service.CacheSyncService;
import com.waveai.worker.service.CacheSyncService.SyncResult;
import com.waveai.worker.service.RateLimitedException;
import com.waveai.worker.service.UpstreamRateLimiter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final CacheSyncService cacheSyncService;
    private final UpstreamRateLimiter rateLimiter;
    private final SessionRepository sessionRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final FolderRepository folderRepository;

    public SyncController(
            CacheSyncService cacheSyncService,
            UpstreamRateLimiter rateLimiter,
            SessionRepository sessionRepository,
            SessionDetailRepository sessionDetailRepository,
            FolderRepository folderRepository) {
        this.cacheSyncService = cacheSyncService;
        this.rateLimiter = rateLimiter;
        this.sessionRepository = sessionRepository;
        this.sessionDetailRepository = sessionDetailRepository;
        this.folderRepository = folderRepository;
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> syncSessions() {
        failFastIfPaused();
        SyncResult result = cacheSyncService.syncSessionsNow();
        return respond(result, sessionRepository.findMaxCachedAt().orElse(null));
    }

    @PostMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Object>> syncSession(@PathVariable String id) {
        failFastIfPaused();
        SyncResult result = cacheSyncService.syncSessionDetailNow(id);
        Instant lastSyncedAt = sessionDetailRepository.findById(id)
                .map(SessionDetailEntity::getCachedAt)
                .orElse(null);
        return respond(result, lastSyncedAt);
    }

    @PostMapping("/folders")
    public ResponseEntity<Map<String, Object>> syncFolders() {
        failFastIfPaused();
        SyncResult result = cacheSyncService.syncFoldersNow();
        return respond(result, folderRepository.findMaxCachedAt().orElse(null));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new HashMap<>();
        body.put("sessions", sessionRepository.findMaxCachedAt().orElse(null));
        body.put("folders", folderRepository.findMaxCachedAt().orElse(null));
        body.put("rateLimitedForSeconds", rateLimiter.isPaused() ? rateLimiter.remaining().toSeconds() : 0);
        return body;
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimited(RateLimitedException e) {
        long seconds = Math.max(1, rateLimiter.remaining().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(Map.of("error", "rate-limited", "retryAfterSeconds", seconds));
    }

    private void failFastIfPaused() {
        if (rateLimiter.isPaused()) {
            throw new RateLimitedException(rateLimiter.remaining(), null);
        }
    }

    private ResponseEntity<Map<String, Object>> respond(SyncResult result, Instant lastSyncedAt) {
        if (result == SyncResult.ALREADY_IN_PROGRESS) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", "in-progress"));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("lastSyncedAt", lastSyncedAt);
        return ResponseEntity.ok(body);
    }
}
