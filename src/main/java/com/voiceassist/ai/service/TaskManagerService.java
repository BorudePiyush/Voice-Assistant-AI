package com.voiceassist.ai.service;

import com.voiceassist.ai.model.TaskItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task Manager Service
 * Provides in-memory task and reminder tracking, with natural language intent recognition.
 */
@Service
public class TaskManagerService {

    private static final Logger logger = LoggerFactory.getLogger(TaskManagerService.class);
    private final Map<String, List<TaskItem>> sessionTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskCounter = new AtomicLong(100);

    private static final Pattern ADD_TASK_PATTERN = Pattern.compile(
            "^(?:add(?:\\s+a)?\\s+task|remind\\s+me\\s+to|todo|create(?:\\s+a)?\\s+task)(?:\\s*:\\s*|\\s+)(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    public List<TaskItem> getTasks(String sessionId) {
        if (sessionId == null) return Collections.emptyList();
        List<TaskItem> list = sessionTasks.get(sessionId);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public TaskItem addTask(String sessionId, String title, String priority) {
        if (sessionId == null) sessionId = "default";
        List<TaskItem> list = sessionTasks.computeIfAbsent(sessionId, k -> new ArrayList<>());

        TaskItem item = TaskItem.builder()
                .id("task-" + taskCounter.incrementAndGet())
                .sessionId(sessionId)
                .title(title.trim())
                .completed(false)
                .createdAt(System.currentTimeMillis())
                .priority(priority != null ? priority : "medium")
                .build();

        synchronized (list) {
            list.add(item);
        }
        return item;
    }

    public boolean toggleTask(String sessionId, String taskId) {
        if (sessionId == null || taskId == null) return false;
        List<TaskItem> list = sessionTasks.get(sessionId);
        if (list == null) return false;

        synchronized (list) {
            for (TaskItem item : list) {
                if (taskId.equals(item.getId())) {
                    item.setCompleted(!item.isCompleted());
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deleteTask(String sessionId, String taskId) {
        if (sessionId == null || taskId == null) return false;
        List<TaskItem> list = sessionTasks.get(sessionId);
        if (list == null) return false;

        synchronized (list) {
            return list.removeIf(item -> taskId.equals(item.getId()));
        }
    }

    public void clearTasks(String sessionId) {
        if (sessionId != null) {
            List<TaskItem> list = sessionTasks.get(sessionId);
            if (list != null) {
                synchronized (list) {
                    list.clear();
                }
            }
        }
    }

    /**
     * Parse task commands directly from voice or chat text.
     */
    public TaskActionResult handleNaturalLanguage(String input, String sessionId) {
        if (input == null || input.isBlank()) return null;
        String normalized = input.trim().toLowerCase(Locale.ROOT);

        // Check if adding task
        Matcher addMatcher = ADD_TASK_PATTERN.matcher(input.trim());
        if (addMatcher.find()) {
            String taskTitle = addMatcher.group(1).trim();
            if (!taskTitle.isBlank()) {
                TaskItem created = addTask(sessionId, taskTitle, "medium");
                String responseText = "✅ Task added: \"" + taskTitle + "\". You can check your tasks drawer anytime!";
                return new TaskActionResult(responseText, "task-added", Map.of(
                        "taskId", created.getId(),
                        "title", created.getTitle(),
                        "tasks", getTasks(sessionId)
                ));
            }
        }

        // Check if listing tasks
        if (normalized.contains("my task") || normalized.contains("show task") || normalized.contains("list task") 
                || normalized.contains("what are my tasks") || normalized.contains("show my todo") 
                || normalized.contains("view task") || normalized.contains("get task")
                || normalized.equals("tasks") || normalized.equals("todo")) {
            List<TaskItem> tasks = getTasks(sessionId);
            if (tasks.isEmpty()) {
                return new TaskActionResult(
                        "You don't have any tasks right now. Try saying \"Add task: review code tomorrow\"!",
                        "task-list",
                        Map.of("tasks", tasks)
                );
            }

            StringBuilder sb = new StringBuilder("📋 Here are your tasks:\n\n");
            int i = 1;
            for (TaskItem t : tasks) {
                String status = t.isCompleted() ? "~~[x] " + t.getTitle() + "~~" : "[ ] " + t.getTitle();
                sb.append(i++).append(". ").append(status).append("\n");
            }
            return new TaskActionResult(sb.toString().trim(), "task-list", Map.of("tasks", tasks));
        }

        // Check if clearing tasks
        if (normalized.contains("clear all tasks") || normalized.contains("delete all tasks") || normalized.contains("clear tasks") || normalized.contains("delete tasks")) {
            clearTasks(sessionId);
            return new TaskActionResult("🗑️ All tasks have been cleared.", "task-cleared", Map.of("tasks", Collections.emptyList()));
        }

        return null;
    }

    public record TaskActionResult(String replyText, String intent, Map<String, Object> data) {}
}
