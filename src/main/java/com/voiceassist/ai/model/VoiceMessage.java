package com.voiceassist.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Voice Message Model
 * Represents a voice message in the system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceMessage {

    @JsonProperty("type")
    private String type; // e.g., "audio", "text", "command"

    @JsonProperty("content")
    private String content; // Base64 encoded audio or text content

    @JsonProperty("language")
    private String language; // ISO 639-1 language code (e.g., "en")

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("sessionId")
    private String sessionId;
}
