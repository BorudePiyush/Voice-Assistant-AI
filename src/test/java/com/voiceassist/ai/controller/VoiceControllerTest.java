package com.voiceassist.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceassist.ai.model.VoiceMessage;
import com.voiceassist.ai.service.VoiceProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for VoiceController
 */
public class VoiceControllerTest {

    private MockMvc mockMvc;
    private VoiceProcessingService voiceProcessingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        voiceProcessingService = mock(VoiceProcessingService.class);
        VoiceController voiceController = new VoiceController(voiceProcessingService);
        mockMvc = MockMvcBuilders.standaloneSetup(voiceController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testProcessVoice() throws Exception {
        VoiceMessage message = VoiceMessage.builder()
                .type("text")
                .content("hello")
                .language("en")
                .sessionId("session-1")
                .build();

        String jsonResponse = "{\"status\":\"success\",\"message\":\"Hello there!\",\"timestamp\":123456789}";
        when(voiceProcessingService.processVoice(any(VoiceMessage.class))).thenReturn(jsonResponse);

        mockMvc.perform(post("/api/v1/voice/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Hello there!"));
    }

    @Test
    public void testRecognizeVoice() throws Exception {
        VoiceMessage message = VoiceMessage.builder()
                .type("audio")
                .content("SGVsbG8=")
                .language("en")
                .sessionId("session-1")
                .build();

        String jsonResponse = "{\"status\":\"success\",\"message\":\"Audio processed\",\"timestamp\":123456789}";
        when(voiceProcessingService.processVoice(any(VoiceMessage.class))).thenReturn(jsonResponse);

        mockMvc.perform(post("/api/v1/voice/recognize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Audio processed"));
    }
}
