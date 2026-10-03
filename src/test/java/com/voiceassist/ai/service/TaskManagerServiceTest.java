package com.voiceassist.ai.service;

import com.voiceassist.ai.model.TaskItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskManagerService
 */
public class TaskManagerServiceTest {

    private TaskManagerService taskManagerService;

    @BeforeEach
    public void setUp() {
        taskManagerService = new TaskManagerService();
    }

    @Test
    public void testAddTaskAndGetTasks() {
        TaskItem item = taskManagerService.addTask("session-1", "Buy groceries", "high");
        assertNotNull(item);
        assertEquals("Buy groceries", item.getTitle());
        assertEquals("high", item.getPriority());
        assertFalse(item.isCompleted());

        List<TaskItem> tasks = taskManagerService.getTasks("session-1");
        assertEquals(1, tasks.size());
        assertEquals("Buy groceries", tasks.get(0).getTitle());
    }

    @Test
    public void testToggleTask() {
        TaskItem item = taskManagerService.addTask("session-1", "Write documentation", "medium");
        assertFalse(item.isCompleted());

        boolean toggled = taskManagerService.toggleTask("session-1", item.getId());
        assertTrue(toggled);

        List<TaskItem> tasks = taskManagerService.getTasks("session-1");
        assertTrue(tasks.get(0).isCompleted());

        // Toggle back
        taskManagerService.toggleTask("session-1", item.getId());
        tasks = taskManagerService.getTasks("session-1");
        assertFalse(tasks.get(0).isCompleted());
    }

    @Test
    public void testDeleteTask() {
        TaskItem item1 = taskManagerService.addTask("session-1", "Task 1", "low");
        TaskItem item2 = taskManagerService.addTask("session-1", "Task 2", "medium");

        assertEquals(2, taskManagerService.getTasks("session-1").size());

        boolean deleted = taskManagerService.deleteTask("session-1", item1.getId());
        assertTrue(deleted);

        List<TaskItem> remaining = taskManagerService.getTasks("session-1");
        assertEquals(1, remaining.size());
        assertEquals("Task 2", remaining.get(0).getTitle());
    }

    @Test
    public void testClearTasks() {
        taskManagerService.addTask("session-1", "Task A", "low");
        taskManagerService.addTask("session-1", "Task B", "high");

        assertEquals(2, taskManagerService.getTasks("session-1").size());

        taskManagerService.clearTasks("session-1");
        assertTrue(taskManagerService.getTasks("session-1").isEmpty());
    }

    @Test
    public void testNaturalLanguageAddTask() {
        TaskManagerService.TaskActionResult result = taskManagerService.handleNaturalLanguage("Add task: Submit weekly report", "session-1");
        assertNotNull(result);
        assertEquals("task-added", result.intent());
        assertTrue(result.replyText().contains("Submit weekly report"));

        List<TaskItem> tasks = taskManagerService.getTasks("session-1");
        assertEquals(1, tasks.size());
        assertEquals("Submit weekly report", tasks.get(0).getTitle());
    }

    @Test
    public void testNaturalLanguageShowTasks() {
        taskManagerService.addTask("session-1", "Review PR", "high");

        TaskManagerService.TaskActionResult result = taskManagerService.handleNaturalLanguage("show my tasks", "session-1");
        assertNotNull(result);
        assertEquals("task-list", result.intent());
        assertTrue(result.replyText().contains("Review PR"));
    }

    @Test
    public void testNaturalLanguageClearTasks() {
        taskManagerService.addTask("session-1", "Review PR", "high");

        TaskManagerService.TaskActionResult result = taskManagerService.handleNaturalLanguage("clear all tasks", "session-1");
        assertNotNull(result);
        assertEquals("task-cleared", result.intent());
        assertTrue(taskManagerService.getTasks("session-1").isEmpty());
    }
}
