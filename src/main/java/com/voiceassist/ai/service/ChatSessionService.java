package com.voiceassist.ai.service;

import com.voiceassist.ai.model.ChatMessage;
import com.voiceassist.ai.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chat Session Service
 * Manages conversational multi-turn sessions, chat history, and exports.
 */
@Service
public class ChatSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ChatSessionService.class);
    private static final int MAX_MESSAGES_PER_SESSION = 100;
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();
    private final AtomicLong messageCounter = new AtomicLong(0);

    public ChatSession getOrCreateSession(String sessionId, String persona) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        }

        final String finalSessionId = sessionId;
        return sessions.computeIfAbsent(finalSessionId, id -> {
            long now = System.currentTimeMillis();
            return ChatSession.builder()
                    .id(id)
                    .title("New Conversation")
                    .createdAt(now)
                    .updatedAt(now)
                    .persona(persona != null && !persona.isBlank() ? persona : "jarvis")
                    .messageCount(0)
                    .messages(new ArrayList<>())
                    .build();
        });
    }

    public Optional<ChatSession> getSession(String sessionId) {
        if (sessionId == null) return Optional.empty();
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public List<ChatSession> getAllSessions() {
        List<ChatSession> list = new ArrayList<>(sessions.values());
        list.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return list;
    }

    public synchronized void addMessage(String sessionId, ChatMessage message) {
        ChatSession session = getOrCreateSession(sessionId, message.getPersona());
        if (message.getId() == null || message.getId().isBlank()) {
            message.setId("msg-" + messageCounter.incrementAndGet());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }

        List<ChatMessage> list = session.getMessages();
        list.add(message);

        // Limit size if too long
        if (list.size() > MAX_MESSAGES_PER_SESSION) {
            list.remove(0);
        }

        session.setMessageCount(list.size());
        session.setUpdatedAt(System.currentTimeMillis());

        if (message.getContent() != null && !message.getContent().isBlank()) {
            String preview = message.getContent().trim();
            if (preview.length() > 60) {
                preview = preview.substring(0, 57) + "...";
            }
            session.setLastMessagePreview(preview);

            // If session still has default title and this is the first user message, generate title
            if ("New Conversation".equals(session.getTitle()) && "user".equalsIgnoreCase(message.getRole())) {
                String title = message.getContent().trim();
                if (title.length() > 32) {
                    title = title.substring(0, 30) + "...";
                }
                session.setTitle(capitalize(title));
            }
        }
    }

    public List<ChatMessage> getHistory(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session == null) {
            return Collections.emptyList();
        }
        synchronized (session) {
            return new ArrayList<>(session.getMessages());
        }
    }

    public boolean clearHistory(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            synchronized (session) {
                session.getMessages().clear();
                session.setMessageCount(0);
                session.setLastMessagePreview("");
                session.setUpdatedAt(System.currentTimeMillis());
            }
            return true;
        }
        return false;
    }

    public boolean deleteSession(String sessionId) {
        return sessions.remove(sessionId) != null;
    }

    public boolean updateTitle(String sessionId, String newTitle) {
        ChatSession session = sessions.get(sessionId);
        if (session != null && newTitle != null && !newTitle.isBlank()) {
            session.setTitle(newTitle.trim());
            session.setUpdatedAt(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public String exportSessionAsMarkdown(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session == null) {
            return "# Chat Session Not Found\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Voice Assist AI - Chat Transcript\n\n");
        sb.append("**Session ID:** `").append(session.getId()).append("`  \n");
        sb.append("**Title:** ").append(session.getTitle()).append("  \n");
        sb.append("**Created:** ").append(new Date(session.getCreatedAt())).append("  \n");
        sb.append("**Persona:** ").append(session.getPersona()).append("\n\n---\n\n");

        for (ChatMessage msg : session.getMessages()) {
            String role = "user".equalsIgnoreCase(msg.getRole()) ? "👤 **You**" : "🤖 **Voice Assist AI**";
            sb.append(role).append(" (").append(new Date(msg.getTimestamp())).append("):\n\n");
            sb.append(msg.getContent()).append("\n\n---\n\n");
        }

        return sb.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + (str.length() > 1 ? str.substring(1) : "");
    }
}
