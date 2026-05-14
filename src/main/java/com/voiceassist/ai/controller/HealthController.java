package com.voiceassist.ai.controller;

import org.springframework.web.bind.annotation.*;
import com.voiceassist.ai.model.VoiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health Check Controller
 * Provides REST endpoints for application health checks and status.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public VoiceResponse health() {
        logger.info("Health check request received");

        return VoiceResponse.builder()
                .status("success")
                .message("Application is running")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Status endpoint.
     */
    @GetMapping("/status")
    public VoiceResponse status() {
        logger.info("Status check request received");

        return VoiceResponse.builder()
                .status("success")
                .message("Voice Assist AI is operational")
                .data("WebSocket endpoint: /ws/voice")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
