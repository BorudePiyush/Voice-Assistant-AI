package com.voiceassist.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceassist.ai.model.VoiceMessage;
import com.voiceassist.ai.service.VoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Voice & Chat WebSocket Handler
 * Handles real-time voice and text chat streams over WebSocket.
 */
@Component
public class VoiceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoiceWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final VoiceProcessingService voiceProcessingService;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public VoiceWebSocketHandler(VoiceProcessingService voiceProcessingService) {
        this.voiceProcessingService = voiceProcessingService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            logger.info("WebSocket incoming payload: {}", payload);

            VoiceMessage voiceMessage;
            try {
                voiceMessage = objectMapper.readValue(payload, VoiceMessage.class);
            } catch (Exception e) {
                // If raw text was sent instead of JSON
                voiceMessage = VoiceMessage.builder()
                        .type("text")
                        .content(payload)
                        .language("en")
                        .sessionId(session.getId())
                        .timestamp(System.currentTimeMillis())
                        .build();
            }

            if (voiceMessage.getSessionId() == null || voiceMessage.getSessionId().isBlank()) {
                voiceMessage.setSessionId(session.getId());
            }

            // Process message
            String response = voiceProcessingService.processVoice(voiceMessage);

            // Send response back
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(response));
            }
        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage("{\"status\": \"error\", \"message\": \"Failed to process WebSocket message: " + e.getMessage() + "\"}"));
            }
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        activeSessions.put(session.getId(), session);
        logger.info("WebSocket connection established: session={}", session.getId());

        // Send welcome acknowledgment
        String welcome = "{\"status\": \"connected\", \"sessionId\": \"" + session.getId() + "\", \"message\": \"WebSocket stream connected.\"}";
        session.sendMessage(new TextMessage(welcome));
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        activeSessions.remove(session.getId());
        logger.info("WebSocket connection closed: session={}, status={}", session.getId(), status);
    }
}
