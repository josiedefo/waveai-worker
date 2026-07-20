package com.waveai.worker.controller;

import com.waveai.worker.repository.FolderRepository;
import com.waveai.worker.repository.SessionDetailRepository;
import com.waveai.worker.repository.SessionRepository;
import com.waveai.worker.service.CacheSyncService;
import com.waveai.worker.service.CacheSyncService.SyncResult;
import com.waveai.worker.service.RateLimitedException;
import com.waveai.worker.service.UpstreamRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock CacheSyncService cacheSyncService;
    @Mock UpstreamRateLimiter rateLimiter;
    @Mock SessionRepository sessionRepository;
    @Mock SessionDetailRepository sessionDetailRepository;
    @Mock FolderRepository folderRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SyncController controller = new SyncController(
                cacheSyncService, rateLimiter, sessionRepository, sessionDetailRepository, folderRepository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void syncSessions_ok() throws Exception {
        when(rateLimiter.isPaused()).thenReturn(false);
        when(cacheSyncService.syncSessionsNow()).thenReturn(SyncResult.COMPLETED);
        when(sessionRepository.findMaxCachedAt()).thenReturn(Optional.of(Instant.now()));

        mvc.perform(post("/api/sync/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void syncSessions_alreadyInProgress_returns202() throws Exception {
        when(rateLimiter.isPaused()).thenReturn(false);
        when(cacheSyncService.syncSessionsNow()).thenReturn(SyncResult.ALREADY_IN_PROGRESS);

        mvc.perform(post("/api/sync/sessions"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("in-progress"));
    }

    @Test
    void syncSessions_whilePaused_failsFastWith429() throws Exception {
        when(rateLimiter.isPaused()).thenReturn(true);
        when(rateLimiter.remaining()).thenReturn(Duration.ofSeconds(42));

        mvc.perform(post("/api/sync/sessions"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.error").value("rate-limited"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(42));

        verify(cacheSyncService, never()).syncSessionsNow();
    }

    @Test
    void syncSession_upstream429_mapsTo429Response() throws Exception {
        when(rateLimiter.isPaused()).thenReturn(false);
        when(cacheSyncService.syncSessionDetailNow("s1"))
                .thenThrow(new RateLimitedException(Duration.ofSeconds(30), null));
        when(rateLimiter.remaining()).thenReturn(Duration.ofSeconds(30));

        mvc.perform(post("/api/sync/sessions/s1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"));
    }

    @Test
    void status_reportsLastSyncTimes() throws Exception {
        when(sessionRepository.findMaxCachedAt()).thenReturn(Optional.of(Instant.parse("2026-07-19T10:00:00Z")));
        when(folderRepository.findMaxCachedAt()).thenReturn(Optional.empty());
        when(rateLimiter.isPaused()).thenReturn(false);

        mvc.perform(get("/api/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimitedForSeconds").value(0));
    }
}
