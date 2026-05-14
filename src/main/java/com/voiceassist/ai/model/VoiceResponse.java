package com.voiceassist.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Voice Response Model
 * Represents a response from the voice processing service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceResponse {

    @JsonProperty("status")
    private String status; // "success" or "error"

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;

    @JsonProperty("timestamp")
    private Long timestamp;
}
