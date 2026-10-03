package com.voiceassist.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat Request Model
 * Represents incoming chat message request from frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("persona")
    private String persona;

    @JsonProperty("apiKey")
    private String apiKey;

    @JsonProperty("model")
    private String model;

    @JsonProperty("language")
    private String language;
}
