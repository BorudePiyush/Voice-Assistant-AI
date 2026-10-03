package com.voiceassist.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceassist.ai.model.TaskItem;
import com.voiceassist.ai.model.VoiceResponse;
import com.voiceassist.ai.service.TaskManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TaskController
 */
public class TaskControllerTest {

    private MockMvc mockMvc;
    private TaskManagerService taskManagerService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        taskManagerService = mock(TaskManagerService.class);
        TaskController taskController = new TaskController(taskManagerService);
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testGetTasks() throws Exception {
        TaskItem item = TaskItem.builder()
                .id("task-101")
                .title("Complete assignment")
                .priority("high")
                .completed(false)
                .createdAt(System.currentTimeMillis())
                .build();

        when(taskManagerService.getTasks("test-session")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/tasks/test-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("task-101"))
                .andExpect(jsonPath("$[0].title").value("Complete assignment"));
    }

    @Test
    public void testAddTaskSuccess() throws Exception {
        TaskItem item = TaskItem.builder()
                .id("task-102")
                .title("New Task")
                .priority("medium")
                .completed(false)
                .createdAt(System.currentTimeMillis())
                .build();

        when(taskManagerService.addTask(eq("test-session"), eq("New Task"), eq("medium"))).thenReturn(item);

        mockMvc.perform(post("/api/v1/tasks/test-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "New Task", "priority", "medium"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Task created successfully"));
    }

    @Test
    public void testAddTaskEmptyTitle() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/test-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Task title cannot be empty"));
    }

    @Test
    public void testToggleTask() throws Exception {
        when(taskManagerService.toggleTask("test-session", "task-101")).thenReturn(true);

        mockMvc.perform(patch("/api/v1/tasks/test-session/task-101/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testDeleteTask() throws Exception {
        when(taskManagerService.deleteTask("test-session", "task-101")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/tasks/test-session/task-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testClearTasks() throws Exception {
        doNothing().when(taskManagerService).clearTasks("test-session");

        mockMvc.perform(delete("/api/v1/tasks/test-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
