package com.waveai.worker.controller;

import com.waveai.worker.dto.FolderDto;
import com.waveai.worker.dto.SessionDetailDto;
import com.waveai.worker.dto.SessionDto;
import com.waveai.worker.dto.TranscriptSegmentDto;
import com.waveai.worker.entity.SessionEntity;
import com.waveai.worker.mapper.SessionMapper;
import com.waveai.worker.repository.FolderRepository;
import com.waveai.worker.repository.SessionRepository;
import com.waveai.worker.repository.TranscriptSegmentRepository;
import com.waveai.worker.service.CacheSyncService;
import com.waveai.worker.sse.SseEmitterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionRepository sessionRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final FolderRepository folderRepository;
    private final CacheSyncService cacheSyncService;
    private final SseEmitterRegistry sseRegistry;
    private final SessionMapper mapper;

    public SessionController(
            SessionRepository sessionRepository,
            TranscriptSegmentRepository transcriptSegmentRepository,
            FolderRepository folderRepository,
            CacheSyncService cacheSyncService,
            SseEmitterRegistry sseRegistry,
            SessionMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.folderRepository = folderRepository;
        this.cacheSyncService = cacheSyncService;
        this.sseRegistry = sseRegistry;
        this.mapper = mapper;
    }

    @GetMapping("/sessions")
    public List<SessionDto> getSessions() {
        cacheSyncService.triggerSessionsSync();
        return sessionRepository.findAll().stream()
            .map(mapper::toSessionDto)
            .toList();
    }

    @GetMapping("/sessions/{id}")
    public SessionDetailDto getSession(@PathVariable String id) {
        cacheSyncService.triggerSessionDetailSync(id);
        SessionEntity session = sessionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        return mapper.toSessionDetailDto(session);
    }

    @GetMapping("/sessions/{id}/transcript")
    public List<TranscriptSegmentDto> getTranscript(@PathVariable String id) {
        return transcriptSegmentRepository.findBySessionIdOrderBySegmentIdxAsc(id).stream()
            .map(mapper::toTranscriptSegmentDto)
            .toList();
    }

    @GetMapping("/folders")
    public List<FolderDto> getFolders() {
        cacheSyncService.triggerFoldersSync();
        return folderRepository.findAll().stream()
            .map(mapper::toFolderDto)
            .toList();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseRegistry.register();
    }
}
