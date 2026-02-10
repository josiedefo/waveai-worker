package com.waveai.worker.service;

import com.waveai.worker.model.Session;
import com.waveai.worker.model.SessionsResponse;
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
