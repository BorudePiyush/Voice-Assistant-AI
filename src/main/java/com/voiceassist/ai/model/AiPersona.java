package com.voiceassist.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Persona Model
 * Defines personality and system prompt preset for the chatbot.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPersona {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("systemPrompt")
    private String systemPrompt;

    @JsonProperty("avatar")
    private String avatar;

    @JsonProperty("color")
    private String color;

    @JsonProperty("tag")
    private String tag;
}
