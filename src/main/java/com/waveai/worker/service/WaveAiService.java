package com.waveai.worker.service;

import com.waveai.worker.model.Folder;
import com.waveai.worker.model.FoldersResponse;
import com.waveai.worker.model.Session;
import com.waveai.worker.model.SessionDetail;
import com.waveai.worker.model.SessionsResponse;
import com.waveai.worker.model.TranscriptResponse;
import com.waveai.worker.model.TranscriptSegment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WaveAiService {

    private final RestClient restClient;

    public WaveAiService(RestClient waveAiRestClient) {
        this.restClient = waveAiRestClient;
    }

    public List<Folder> getFolders() {
        try {
            FoldersResponse response = restClient.get()
                    .uri("/folders")
                    .retrieve()
                    .body(FoldersResponse.class);
            return response != null && response.folders() != null
                    ? response.folders() : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to fetch folders from WaveAI",
                    e
            );
        }
    }

    public SessionDetail getSessionDetail(String id) {
        try {
            return restClient.get()
                    .uri("/sessions/{id}", id)
                    .retrieve()
                    .body(SessionDetail.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to fetch session from WaveAI",
                    e
            );
        }
    }

    public List<TranscriptSegment> getTranscript(String sessionId) {
        try {
            TranscriptResponse response = restClient.get()
                    .uri("/sessions/{id}/transcript", sessionId)
                    .retrieve()
                    .body(TranscriptResponse.class);
            return response != null && response.segments() != null
                    ? response.segments() : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to fetch transcript from WaveAI",
                    e
            );
        }
    }

    public List<Session> getSessions() {
        try {
            SessionsResponse response = restClient.get()
                    .uri("/sessions")
                    .retrieve()
                    .body(SessionsResponse.class);
            return response != null && response.sessions() != null
                    ? response.sessions() : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to fetch sessions from WaveAI",
                    e
            );
        }
    }
}
