package com.waveai.worker.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class WaveAiServiceRateLimitTest {

    @Test
    void tooManyRequests_mapsToRateLimitedExceptionWithRetryAfter() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "30");
        server.expect(requestTo("/sessions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));
        WaveAiService service = new WaveAiService(builder.build());

        assertThatThrownBy(service::getSessions)
                .isInstanceOf(RateLimitedException.class)
                .satisfies(e -> assertThat(((RateLimitedException) e).retryAfter())
                        .isEqualTo(Duration.ofSeconds(30)));
    }

    @Test
    void tooManyRequests_withoutRetryAfter_yieldsNullDuration() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/folders"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        WaveAiService service = new WaveAiService(builder.build());

        assertThatThrownBy(service::getFolders)
                .isInstanceOf(RateLimitedException.class)
                .satisfies(e -> assertThat(((RateLimitedException) e).retryAfter()).isNull());
    }

    @Test
    void otherUpstreamErrors_stayBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/sessions")).andRespond(withServerError());
        WaveAiService service = new WaveAiService(builder.build());

        assertThatThrownBy(service::getSessions)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
