package com.voiceassist.ai.controller;

import com.voiceassist.ai.model.TaskItem;
import com.voiceassist.ai.model.VoiceResponse;
import com.voiceassist.ai.service.TaskManagerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Task Management REST Controller
 * Provides endpoints for tasks, todos, and reminders.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskManagerService taskManagerService;

    public TaskController(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }

    @GetMapping("/{sessionId}")
    public List<TaskItem> getTasks(@PathVariable String sessionId) {
        return taskManagerService.getTasks(sessionId);
    }

    @PostMapping("/{sessionId}")
    public VoiceResponse addTask(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String priority = body.getOrDefault("priority", "medium");

        if (title.isBlank()) {
            return VoiceResponse.builder()
                    .status("error")
                    .message("Task title cannot be empty")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        TaskItem item = taskManagerService.addTask(sessionId, title, priority);
        return VoiceResponse.builder()
                .status("success")
                .message("Task created successfully")
                .data(item)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @PatchMapping("/{sessionId}/{taskId}/toggle")
    public VoiceResponse toggleTask(@PathVariable String sessionId, @PathVariable String taskId) {
        boolean toggled = taskManagerService.toggleTask(sessionId, taskId);
        return VoiceResponse.builder()
                .status(toggled ? "success" : "not_found")
                .message(toggled ? "Task toggled" : "Task not found")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @DeleteMapping("/{sessionId}/{taskId}")
    public VoiceResponse deleteTask(@PathVariable String sessionId, @PathVariable String taskId) {
        boolean deleted = taskManagerService.deleteTask(sessionId, taskId);
        return VoiceResponse.builder()
                .status(deleted ? "success" : "not_found")
                .message(deleted ? "Task deleted" : "Task not found")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @DeleteMapping("/{sessionId}")
    public VoiceResponse clearTasks(@PathVariable String sessionId) {
        taskManagerService.clearTasks(sessionId);
        return VoiceResponse.builder()
                .status("success")
                .message("All tasks cleared")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
