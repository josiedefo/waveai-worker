package com.waveai.worker.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaForwardControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SpaForwardController()).build();
    }

    @Test
    void sessionDeepLink_forwardsToIndex() throws Exception {
        mvc.perform(get("/session/abc-123"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void foldersDeepLink_forwardsToIndex() throws Exception {
        mvc.perform(get("/folders"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
