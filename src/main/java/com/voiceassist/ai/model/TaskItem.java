package com.voiceassist.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task Item Model
 * Represents a todo task or reminder created via voice or text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskItem {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("completed")
    private boolean completed;

    @JsonProperty("createdAt")
    private Long createdAt;

    @JsonProperty("priority")
    private String priority; // "low", "medium", "high"
}
