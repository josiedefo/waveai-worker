package com.waveai.worker.service;

import com.waveai.worker.config.SyncProperties;
import com.waveai.worker.entity.SessionDetailEntity;
import com.waveai.worker.entity.SessionEntity;
import com.waveai.worker.model.Session;
import com.waveai.worker.repository.FolderRepository;
import com.waveai.worker.repository.SessionDetailRepository;
import com.waveai.worker.repository.SessionRepository;
import com.waveai.worker.repository.TranscriptSegmentRepository;
import com.waveai.worker.sse.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheSyncServiceTest {

    @Mock WaveAiService waveAiService;
    @Mock SessionRepository sessionRepository;
    @Mock SessionDetailRepository sessionDetailRepository;
    @Mock TranscriptSegmentRepository transcriptSegmentRepository;
    @Mock FolderRepository folderRepository;
    @Mock SseEmitterRegistry sseRegistry;
    @Mock UpstreamRateLimiter rateLimiter;
    @Mock ThreadPoolTaskExecutor syncExecutor;

    private CacheSyncService service;

    @BeforeEach
    void setUp() {
        SyncProperties props = new SyncProperties(
                "0 0 * * * *",
                Duration.ofSeconds(60),
                new SyncProperties.Ttl(Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30)));
        service = new CacheSyncService(
                waveAiService, sessionRepository, sessionDetailRepository,
                transcriptSegmentRepository, folderRepository, sseRegistry,
                props, rateLimiter, syncExecutor);
    }

    private void executorRunsInline() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(syncExecutor).execute(any(Runnable.class));
    }

    @Test
    void triggerSessionsSync_skipsWhenCacheFresh() {
        when(rateLimiter.isPaused()).thenReturn(false);
        when(sessionRepository.findMaxCachedAt()).thenReturn(Optional.of(Instant.now()));

        service.triggerSessionsSync();

        verifyNoInteractions(waveAiService, syncExecutor);
    }

    @Test
    void triggerSessionsSync_skipsWhenRateLimited() {
        when(rateLimiter.isPaused()).thenReturn(true);

        service.triggerSessionsSync();

        verifyNoInteractions(waveAiService, syncExecutor, sessionRepository);
    }

    @Test
    void triggerSessionsSync_syncsWhenStale() {
        executorRunsInline();
        when(rateLimiter.isPaused()).thenReturn(false);
        when(sessionRepository.findMaxCachedAt()).thenReturn(Optional.empty());
        Session session = new Session("s1", "Standup", Instant.now(), 600L, "recording", "zoom");
        when(waveAiService.getSessions()).thenReturn(List.of(session));

        service.triggerSessionsSync();

        verify(waveAiService).getSessions();
        verify(sessionRepository).saveAll(anyList());
        verify(sseRegistry).broadcast(eq("sessions-updated"), any());
    }

    @Test
    void triggerSessionsSync_recordsCooldownOn429_withoutPropagating() {
        executorRunsInline();
        when(rateLimiter.isPaused()).thenReturn(false);
        when(sessionRepository.findMaxCachedAt()).thenReturn(Optional.empty());
        when(waveAiService.getSessions())
                .thenThrow(new RateLimitedException(Duration.ofSeconds(30), null));

        service.triggerSessionsSync();

        verify(rateLimiter).recordRateLimit(Duration.ofSeconds(30));
        verify(sseRegistry, never()).broadcast(any(), any());
    }

    @Test
    void triggerSessionDetailSync_skipsWhenDetailFreshAndTranscriptPresent() {
        when(rateLimiter.isPaused()).thenReturn(false);
        SessionDetailEntity detail = new SessionDetailEntity();
        detail.setCachedAt(Instant.now());
        when(sessionDetailRepository.findById("s1")).thenReturn(Optional.of(detail));
        when(transcriptSegmentRepository.existsBySessionId("s1")).thenReturn(true);

        service.triggerSessionDetailSync("s1");

        verifyNoInteractions(waveAiService, syncExecutor);
    }

    @Test
    void triggerSessionDetailSync_syncsOnlyTranscriptWhenDetailFreshButTranscriptMissing() {
        executorRunsInline();
        when(rateLimiter.isPaused()).thenReturn(false);
        SessionDetailEntity detail = new SessionDetailEntity();
        detail.setCachedAt(Instant.now());
        when(sessionDetailRepository.findById("s1")).thenReturn(Optional.of(detail));
        when(transcriptSegmentRepository.existsBySessionId("s1")).thenReturn(false);
        SessionEntity sessionEntity = new SessionEntity();
        sessionEntity.setId("s1");
        when(sessionRepository.findById("s1")).thenReturn(Optional.of(sessionEntity));
        when(waveAiService.getTranscript("s1")).thenReturn(List.of());

        service.triggerSessionDetailSync("s1");

        verify(waveAiService, never()).getSessionDetail(any());
        verify(waveAiService).getTranscript("s1");
        verify(sseRegistry).broadcast(eq("transcript-updated"), eq("s1"));
    }

    @Test
    void syncSessionsNow_bypassesTtlAndReturnsCompleted() {
        Session session = new Session("s1", "Standup", Instant.now(), 600L, "recording", "zoom");
        when(waveAiService.getSessions()).thenReturn(List.of(session));

        CacheSyncService.SyncResult result = service.syncSessionsNow();

        assertThat(result).isEqualTo(CacheSyncService.SyncResult.COMPLETED);
        verify(sessionRepository).saveAll(anyList());
        // Manual sync never consults the TTL gate.
        verify(sessionRepository, never()).findMaxCachedAt();
    }

    @Test
    void syncSessionsNow_recordsCooldownAndPropagates429() {
        when(waveAiService.getSessions())
                .thenThrow(new RateLimitedException(Duration.ofSeconds(30), null));

        assertThatThrownBy(() -> service.syncSessionsNow())
                .isInstanceOf(RateLimitedException.class);

        verify(rateLimiter).recordRateLimit(Duration.ofSeconds(30));
    }
}
