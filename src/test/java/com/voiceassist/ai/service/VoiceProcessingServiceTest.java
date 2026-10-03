package com.voiceassist.ai.service;

import com.voiceassist.ai.model.ChatRequest;
import com.voiceassist.ai.model.VoiceMessage;
import com.voiceassist.ai.model.VoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for VoiceProcessingService
 */
public class VoiceProcessingServiceTest {

    private VoiceProcessingService voiceProcessingService;
    private ChatSessionService chatSessionService;
    private TaskManagerService taskManagerService;

    @BeforeEach
    public void setUp() {
        chatSessionService = new ChatSessionService();
        taskManagerService = new TaskManagerService();
        voiceProcessingService = new VoiceProcessingService(
                "London",
                "gemini",
                "",
                "gemini-1.5-flash",
                "You are Voice Assist AI, a concise helpful voice assistant. Answer clearly and directly.",
                chatSessionService,
                taskManagerService
        );
    }

    @Test
    public void testProcessAudio() {
        VoiceMessage message = VoiceMessage.builder()
                .type("audio")
                .content("SGVsbG8=") // Base64 for "Hello"
                .language("en")
                .sessionId("session123")
                .build();

        String response = voiceProcessingService.processVoice(message);
        assertNotNull(response);
        assertTrue(response.contains("success"));
    }

    @Test
    public void testProcessText() {
        VoiceMessage message = VoiceMessage.builder()
                .type("text")
                .content("Hello, how are you?")
                .language("en")
                .sessionId("session123")
                .build();

        String response = voiceProcessingService.processVoice(message);
        assertNotNull(response);
        assertTrue(response.contains("success"));
    }

    @Test
    public void testProcessCommand() {
        VoiceMessage message = VoiceMessage.builder()
                .type("command")
                .content("open youtube")
                .language("en")
                .sessionId("session123")
                .build();

        String response = voiceProcessingService.processVoice(message);
        assertNotNull(response);
        assertTrue(response.contains("success"));
    }

    @Test
    public void testProcessChatRequest() {
        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("Calculate (25 * 4) + 10")
                .persona("jarvis")
                .build();

        VoiceResponse response = voiceProcessingService.processChat(request);
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertTrue(response.getMessage().contains("110"));
    }

    @Test
    public void testTaskIntegration() {
        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("Add task: finish the project presentation")
                .persona("jarvis")
                .build();

        VoiceResponse response = voiceProcessingService.processChat(request);
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertTrue(response.getMessage().toLowerCase().contains("task added"));
        assertEquals(1, taskManagerService.getTasks("test-session").size());
    }

    @Test
    public void testLiveWeatherQuery() {
        ChatRequest request = ChatRequest.builder()
                .sessionId("weather-session")
                .message("What is the weather in Tokyo?")
                .persona("jarvis")
                .build();

        VoiceResponse response = voiceProcessingService.processChat(request);
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertNotNull(response.getMessage());
        assertNotNull(response.getData());
    }

    @Test
    public void testKnowledgeQuestions() {
        ChatRequest request = ChatRequest.builder()
                .sessionId("knowledge-session")
                .message("What is photosynthesis?")
                .persona("jarvis")
                .build();

        VoiceResponse response = voiceProcessingService.processChat(request);
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertTrue(response.getMessage().toLowerCase().contains("photosynthesis") || response.getMessage().toLowerCase().contains("plants"));
    }

    @Test
    public void testUnitConversion() {
        ChatRequest request = ChatRequest.builder()
                .sessionId("convert-session")
                .message("Convert 50 km to miles")
                .persona("newton")
                .build();

        VoiceResponse response = voiceProcessingService.processChat(request);
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertTrue(response.getMessage().contains("31.07"));
    }

    @Test
    public void testWorldTimeQuery() {
        ChatRequest request = ChatRequest.builder()
                .sessionId("time-session")
                .message("What is the time in New York?")
                .persona("jarvis")
                .build();

        VoiceResponse response = voiceProcessingService.processChat(request);
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertTrue(response.getMessage().toLowerCase().contains("new york") || response.getMessage().contains("America/New_York"));
    }
}
