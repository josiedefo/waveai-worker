package com.waveai.worker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards Vue Router deep links to the SPA entry point so direct loads and
 * refreshes work. Routes are listed explicitly (mirroring frontend/src/router)
 * so unknown paths still 404; /api/** is untouched.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/session/{id}", "/folders"})
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
