package com.voiceassist.ai.service;

import com.voiceassist.ai.model.ChatMessage;
import com.voiceassist.ai.model.ChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ChatSessionService
 */
public class ChatSessionServiceTest {

    private ChatSessionService chatSessionService;

    @BeforeEach
    public void setUp() {
        chatSessionService = new ChatSessionService();
    }

    @Test
    public void testCreateAndRetrieveSession() {
        ChatSession session = chatSessionService.getOrCreateSession("session-1", "jarvis");
        assertNotNull(session);
        assertEquals("session-1", session.getId());
        assertEquals("jarvis", session.getPersona());
    }

    @Test
    public void testAddAndGetMessages() {
        chatSessionService.addMessage("session-1", ChatMessage.builder()
                .role("user")
                .content("Hello AI!")
                .build());

        chatSessionService.addMessage("session-1", ChatMessage.builder()
                .role("assistant")
                .content("Hello human!")
                .build());

        List<ChatMessage> history = chatSessionService.getHistory("session-1");
        assertEquals(2, history.size());
        assertEquals("user", history.get(0).getRole());
        assertEquals("assistant", history.get(1).getRole());
    }

    @Test
    public void testClearHistory() {
        chatSessionService.addMessage("session-1", ChatMessage.builder()
                .role("user")
                .content("Hello")
                .build());

        assertTrue(chatSessionService.clearHistory("session-1"));
        assertTrue(chatSessionService.getHistory("session-1").isEmpty());
    }
}
