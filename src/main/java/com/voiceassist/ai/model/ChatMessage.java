package com.voiceassist.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Chat Message Model
 * Represents an individual chat message in a conversation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("role")
    private String role; // "user", "assistant", "system"

    @JsonProperty("content")
    private String content;

    @JsonProperty("intent")
    private String intent;

    @JsonProperty("persona")
    private String persona;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("data")
    private Map<String, Object> data;
}
