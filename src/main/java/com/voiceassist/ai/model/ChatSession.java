package com.voiceassist.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat Session Model
 * Represents a conversation session with message history and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSession {

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("createdAt")
    private Long createdAt;

    @JsonProperty("updatedAt")
    private Long updatedAt;

    @JsonProperty("persona")
    private String persona;

    @JsonProperty("messageCount")
    private int messageCount;

    @JsonProperty("lastMessagePreview")
    private String lastMessagePreview;

    @Builder.Default
    @JsonProperty("messages")
    private List<ChatMessage> messages = new ArrayList<>();
}
