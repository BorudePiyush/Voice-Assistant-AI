package com.voiceassist.ai.controller;

import org.springframework.web.bind.annotation.*;
import com.voiceassist.ai.model.VoiceResponse;
import com.voiceassist.ai.service.ChatSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health Check & Status Controller
 * Provides operational telemetry and metrics.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private final ChatSessionService chatSessionService;
    private final long startTime = System.currentTimeMillis();

    public HealthController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public VoiceResponse health() {
        logger.info("Health check request received");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("service", "Voice Assist AI");

        return VoiceResponse.builder()
                .status("success")
                .message("Application is healthy and running")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Detailed Status endpoint.
     */
    @GetMapping("/status")
    public VoiceResponse status() {
        logger.info("Status check request received");

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        String uptimeStr = String.format("%02dh %02dm %02ds", hours, minutes, seconds);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("application", "Voice Assist AI & Chatbot");
        stats.put("version", "2.0.0");
        stats.put("javaVersion", System.getProperty("java.version"));
        stats.put("os", System.getProperty("os.name"));
        stats.put("uptime", uptimeStr);
        stats.put("activeSessions", chatSessionService.getAllSessions().size());
        stats.put("memoryUsedMb", usedMemory);
        stats.put("memoryTotalMb", totalMemory);
        stats.put("memoryMaxMb", maxMemory);
        stats.put("webSocketEndpoint", "/ws/voice");
        stats.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());

        return VoiceResponse.builder()
                .status("success")
                .message("Voice Assist AI is fully operational")
                .data(stats)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
