package com.voiceassist.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.voiceassist.ai.model.VoiceMessage;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for VoiceProcessingService
 */
public class VoiceProcessingServiceTest {

    private VoiceProcessingService voiceProcessingService;

    @BeforeEach
    public void setUp() {
        voiceProcessingService = new VoiceProcessingService(
                "London",
                "gemini",
                "",
                "gemini-1.5-flash",
                "You are Voice Assist AI, a concise helpful voice assistant. Answer clearly and directly."
        );
    }

    @Test
    public void testProcessAudio() {
        VoiceMessage message = VoiceMessage.builder()
                .type("audio")
                .content("base64encodedaudio")
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
                .content("turn on light")
                .language("en")
                .sessionId("session123")
                .build();

        String response = voiceProcessingService.processVoice(message);
        assertNotNull(response);
        assertTrue(response.contains("success"));
    }

    @Test
    public void testProcessInvalidType() {
        VoiceMessage message = VoiceMessage.builder()
                .type("invalid")
                .content("test content")
                .language("en")
                .sessionId("session123")
                .build();

        String response = voiceProcessingService.processVoice(message);
        assertNotNull(response);
        assertTrue(response.contains("error"));
    }
}
