package com.waveai.worker.controller;

import com.waveai.worker.model.Session;
import com.waveai.worker.service.WaveAiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final WaveAiService waveAiService;

    public SessionController(WaveAiService waveAiService) {
        this.waveAiService = waveAiService;
    }

    @GetMapping("/sessions")
    public List<Session> getSessions() {
        return waveAiService.getSessions();
    }
}
