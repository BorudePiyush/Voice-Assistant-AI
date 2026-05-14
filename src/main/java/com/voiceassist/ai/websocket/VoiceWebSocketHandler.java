package com.voiceassist.ai.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceassist.ai.model.VoiceMessage;
import com.voiceassist.ai.service.VoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Voice WebSocket Handler
 * Handles real-time voice communication over WebSocket connections.
 */
@Component
public class VoiceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoiceWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final VoiceProcessingService voiceProcessingService;

    public VoiceWebSocketHandler(VoiceProcessingService voiceProcessingService) {
        this.voiceProcessingService = voiceProcessingService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            VoiceMessage voiceMessage = objectMapper.readValue(payload, VoiceMessage.class);

            logger.info("Received voice message: {}", voiceMessage.getType());

            // Process the voice message
            String response = voiceProcessingService.processVoice(voiceMessage);

            // Send response back to client
            session.sendMessage(new TextMessage(response));
        } catch (Exception e) {
            logger.error("Error processing voice message", e);
            session.sendMessage(new TextMessage("{\"error\": \"Failed to process voice message\"}"));
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {}", session.getId());
    }
}
