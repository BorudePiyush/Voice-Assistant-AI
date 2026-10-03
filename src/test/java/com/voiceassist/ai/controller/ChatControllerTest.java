package com.voiceassist.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceassist.ai.model.ChatMessage;
import com.voiceassist.ai.model.ChatRequest;
import com.voiceassist.ai.model.ChatSession;
import com.voiceassist.ai.model.VoiceResponse;
import com.voiceassist.ai.service.ChatSessionService;
import com.voiceassist.ai.service.VoiceProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChatController
 */
public class ChatControllerTest {

    private MockMvc mockMvc;
    private VoiceProcessingService voiceProcessingService;
    private ChatSessionService chatSessionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        voiceProcessingService = mock(VoiceProcessingService.class);
        chatSessionService = mock(ChatSessionService.class);
        ChatController chatController = new ChatController(voiceProcessingService, chatSessionService);
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testSendMessage() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .sessionId("session-123")
                .message("Hello")
                .persona("jarvis")
                .build();

        VoiceResponse response = VoiceResponse.builder()
                .status("success")
                .message("Hello! How can I assist you?")
                .timestamp(System.currentTimeMillis())
                .build();

        when(voiceProcessingService.processChat(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Hello! How can I assist you?"));
    }

    @Test
    public void testGetSessions() throws Exception {
        ChatSession session = ChatSession.builder()
                .id("session-1")
                .title("Test Chat")
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .persona("jarvis")
                .messageCount(1)
                .messages(Collections.emptyList())
                .build();

        when(chatSessionService.getAllSessions()).thenReturn(List.of(session));

        mockMvc.perform(get("/api/v1/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("session-1"))
                .andExpect(jsonPath("$[0].title").value("Test Chat"));
    }

    @Test
    public void testGetHistory() throws Exception {
        ChatMessage msg = ChatMessage.builder()
                .id("msg-1")
                .role("user")
                .content("Hello")
                .timestamp(System.currentTimeMillis())
                .build();

        when(chatSessionService.getHistory("session-1")).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/v1/chat/history/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello"));
    }

    @Test
    public void testClearHistory() throws Exception {
        when(chatSessionService.clearHistory("session-1")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/chat/history/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testDeleteSession() throws Exception {
        when(chatSessionService.deleteSession("session-1")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testUpdateSessionTitle() throws Exception {
        when(chatSessionService.updateTitle("session-1", "Updated Title")).thenReturn(true);

        mockMvc.perform(patch("/api/v1/chat/sessions/session-1/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated Title"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testExportSession() throws Exception {
        when(chatSessionService.exportSessionAsMarkdown("session-1")).thenReturn("# Markdown Content");

        mockMvc.perform(get("/api/v1/chat/export/session-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("# Markdown Content"));
    }

    @Test
    public void testGetSuggestions() throws Exception {
        mockMvc.perform(get("/api/v1/chat/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['🌦️ Weather & Live']").isArray());
    }
}
