package com.voiceassist.ai.controller;

import com.voiceassist.ai.service.ChatSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for HealthController
 */
public class HealthControllerTest {

    private MockMvc mockMvc;
    private ChatSessionService chatSessionService;

    @BeforeEach
    public void setUp() {
        chatSessionService = mock(ChatSessionService.class);
        HealthController healthController = new HealthController(chatSessionService);
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
    }

    @Test
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    public void testStatusEndpoint() throws Exception {
        when(chatSessionService.getAllSessions()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.application").value("Voice Assist AI & Chatbot"))
                .andExpect(jsonPath("$.data.version").value("2.0.0"));
    }
}
