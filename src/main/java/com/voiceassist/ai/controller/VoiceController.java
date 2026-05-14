package com.voiceassist.ai.controller;

import org.springframework.web.bind.annotation.*;
import com.voiceassist.ai.model.VoiceMessage;
import com.voiceassist.ai.model.VoiceResponse;
import com.voiceassist.ai.service.VoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Voice Controller
 * Provides REST endpoints for voice processing (alternative to WebSocket).
 */
@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceController.class);
    private final VoiceProcessingService voiceProcessingService;

    public VoiceController(VoiceProcessingService voiceProcessingService) {
        this.voiceProcessingService = voiceProcessingService;
    }

    /**
     * Process voice input via REST endpoint.
     */
    @PostMapping("/process")
    public VoiceResponse processVoice(@RequestBody VoiceMessage voiceMessage) {
        logger.info("REST voice processing request: {}", voiceMessage.getType());

        try {
            String response = voiceProcessingService.processVoice(voiceMessage);
            // Parse response back to object
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response, VoiceResponse.class);
        } catch (Exception e) {
            logger.error("Error processing voice", e);
            return VoiceResponse.builder()
                    .status("error")
                    .message("Failed to process voice")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * Recognize speech from audio.
     */
    @PostMapping("/recognize")
    public VoiceResponse recognize(@RequestBody VoiceMessage audioMessage) {
        logger.info("Speech recognition request");

        VoiceMessage textMessage = VoiceMessage.builder()
                .type("text")
                .content(audioMessage.getContent())
                .language(audioMessage.getLanguage())
                .sessionId(audioMessage.getSessionId())
                .build();

        try {
            String response = voiceProcessingService.processVoice(textMessage);
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response, VoiceResponse.class);
        } catch (Exception e) {
            logger.error("Error recognizing speech", e);
            return VoiceResponse.builder()
                    .status("error")
                    .message("Failed to recognize speech")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
}
