package com.voiceassist.ai.controller;

import com.voiceassist.ai.model.AiPersona;
import com.voiceassist.ai.model.ChatMessage;
import com.voiceassist.ai.model.ChatRequest;
import com.voiceassist.ai.model.ChatSession;
import com.voiceassist.ai.model.VoiceResponse;
import com.voiceassist.ai.service.ChatSessionService;
import com.voiceassist.ai.service.VoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Chatbot REST Controller
 * Provides endpoints for multi-turn chat sessions, history, personas, and suggestions.
 */
@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final VoiceProcessingService voiceProcessingService;
    private final ChatSessionService chatSessionService;

    public ChatController(VoiceProcessingService voiceProcessingService, ChatSessionService chatSessionService) {
        this.voiceProcessingService = voiceProcessingService;
        this.chatSessionService = chatSessionService;
    }

    /**
     * Send a message to the AI Chatbot.
     */
    @PostMapping("/message")
    public VoiceResponse sendMessage(@RequestBody ChatRequest request) {
        logger.info("Chat message request in session: {}", request.getSessionId());
        return voiceProcessingService.processChat(request);
    }

    /**
     * Get all conversation sessions.
     */
    @GetMapping("/sessions")
    public List<ChatSession> getSessions() {
        return chatSessionService.getAllSessions();
    }

    /**
     * Get conversation history for a specific session.
     */
    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> getHistory(@PathVariable String sessionId) {
        return chatSessionService.getHistory(sessionId);
    }

    /**
     * Clear message history for a session.
     */
    @DeleteMapping("/history/{sessionId}")
    public VoiceResponse clearHistory(@PathVariable String sessionId) {
        boolean cleared = chatSessionService.clearHistory(sessionId);
        return VoiceResponse.builder()
                .status(cleared ? "success" : "not_found")
                .message(cleared ? "Chat history cleared" : "Session not found")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Delete a conversation session entirely.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public VoiceResponse deleteSession(@PathVariable String sessionId) {
        boolean deleted = chatSessionService.deleteSession(sessionId);
        return VoiceResponse.builder()
                .status(deleted ? "success" : "not_found")
                .message(deleted ? "Session deleted" : "Session not found")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Update title of a session.
     */
    @PatchMapping("/sessions/{sessionId}/title")
    public VoiceResponse updateSessionTitle(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        String newTitle = body.get("title");
        boolean updated = chatSessionService.updateTitle(sessionId, newTitle);
        return VoiceResponse.builder()
                .status(updated ? "success" : "error")
                .message(updated ? "Session title updated" : "Failed to update title")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Export session as Markdown file.
     */
    @GetMapping("/export/{sessionId}")
    public ResponseEntity<String> exportSession(@PathVariable String sessionId) {
        String markdown = chatSessionService.exportSessionAsMarkdown(sessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chat-" + sessionId + ".md\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(markdown);
    }

    /**
     * Get list of all available AI Personas.
     */
    @GetMapping("/personas")
    public List<AiPersona> getPersonas() {
        return voiceProcessingService.getAvailablePersonas();
    }

    /**
     * Get categorized prompt suggestions.
     */
    @GetMapping("/suggestions")
    public Map<String, List<Map<String, String>>> getSuggestions() {
        Map<String, List<Map<String, String>>> categories = new LinkedHashMap<>();

        categories.put("🌦️ Weather & Live", List.of(
                Map.of("label", "Weather in New York", "prompt", "What is the weather in New York?", "icon", "fas fa-cloud-sun"),
                Map.of("label", "Weather in Tokyo", "prompt", "Show weather forecast for Tokyo", "icon", "fas fa-temperature-high"),
                Map.of("label", "Current World Time", "prompt", "What time is it in London right now?", "icon", "fas fa-clock")
        ));

        categories.put("💻 Coding & Tech", List.of(
                Map.of("label", "Java 21 Features", "prompt", "Explain Java 21 Virtual Threads and Record Patterns with code examples", "icon", "fab fa-java"),
                Map.of("label", "Spring Boot REST API", "prompt", "Show me how to build a clean REST controller in Spring Boot 3", "icon", "fas fa-laptop-code"),
                Map.of("label", "Regex Email Matcher", "prompt", "Write a regex expression in Java to validate email addresses", "icon", "fas fa-code")
        ));

        categories.put("🧮 Math & Calculation", List.of(
                Map.of("label", "Compound Interest", "prompt", "Calculate (5000 * (1 + 0.07)^5)", "icon", "fas fa-calculator"),
                Map.of("label", "Unit Conversion", "prompt", "Convert 75 miles to km", "icon", "fas fa-ruler-combined"),
                Map.of("label", "Square Root & Power", "prompt", "Calculate 144^0.5 + 2^8", "icon", "fas fa-square-root-variable")
        ));

        categories.put("📋 Tasks & Productivity", List.of(
                Map.of("label", "Add Todo Task", "prompt", "Add task: Review voice assistant code and test speech features", "icon", "fas fa-tasks"),
                Map.of("label", "Show My Tasks", "prompt", "Show my tasks", "icon", "fas fa-list-check"),
                Map.of("label", "Daily Motivation", "prompt", "Give me a powerful motivational quote for today", "icon", "fas fa-fire")
        ));

        categories.put("🚀 Shortcuts & Fun", List.of(
                Map.of("label", "Open YouTube", "prompt", "Open YouTube", "icon", "fab fa-youtube"),
                Map.of("label", "Open Spotify", "prompt", "Open Spotify", "icon", "fab fa-spotify"),
                Map.of("label", "Programming Joke", "prompt", "Tell me a funny programming joke", "icon", "fas fa-laugh-beam")
        ));

        return categories;
    }
}
