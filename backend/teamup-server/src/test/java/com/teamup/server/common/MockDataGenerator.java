package com.teamup.server.common;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock Data Generator
 * 
 * Provides utilities for generating mock data for tests.
 * Used for creating test fixtures and sample data.
 */
public class MockDataGenerator {

    /**
     * Generates a mock team entity
     */
    public static Map<String, Object> createMockTeam(String teamId) {
        Map<String, Object> team = new HashMap<>();
        team.put("id", teamId);
        team.put("name", "Test Team " + teamId);
        team.put("description", "A test team for unit testing");
        team.put("avatar", "https://example.com/avatar.png");
        team.put("memberCount", 5);
        team.put("createdAt", Timestamp.valueOf(LocalDateTime.now()));
        team.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        return team;
    }

    /**
     * Generates a mock team with custom properties
     */
    public static Map<String, Object> createMockTeam(String teamId, Map<String, Object> overrides) {
        Map<String, Object> team = createMockTeam(teamId);
        team.putAll(overrides);
        return team;
    }

    /**
     * Generates a list of mock teams
     */
    public static List<Map<String, Object>> createMockTeams(int count) {
        List<Map<String, Object>> teams = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            teams.add(createMockTeam("team-" + i));
        }
        return teams;
    }

    /**
     * Generates a mock task entity
     */
    public static Map<String, Object> createMockTask(String taskId, String teamId) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("teamId", teamId);
        task.put("title", "Test Task " + taskId);
        task.put("description", "A test task for unit testing");
        task.put("status", "todo");
        task.put("priority", "medium");
        task.put("assigneeId", "user-1");
        task.put("createdAt", Timestamp.valueOf(LocalDateTime.now()));
        task.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        task.put("dueDate", null);
        return task;
    }

    /**
     * Generates a mock task with custom properties
     */
    public static Map<String, Object> createMockTask(
            String taskId,
            String teamId,
            Map<String, Object> overrides) {
        Map<String, Object> task = createMockTask(taskId, teamId);
        task.putAll(overrides);
        return task;
    }

    /**
     * Generates a list of mock tasks for a team
     */
    public static List<Map<String, Object>> createMockTasks(String teamId, int count) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            tasks.add(createMockTask("task-" + i, teamId));
        }
        return tasks;
    }

    /**
     * Generates a mock user entity
     */
    public static Map<String, Object> createMockUser(String userId) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("username", "testuser" + userId);
        user.put("email", "test" + userId + "@example.com");
        user.put("avatar", "https://example.com/avatar.png");
        user.put("createdAt", Timestamp.valueOf(LocalDateTime.now()));
        return user;
    }

    /**
     * Generates a mock user with custom properties
     */
    public static Map<String, Object> createMockUser(String userId, Map<String, Object> overrides) {
        Map<String, Object> user = createMockUser(userId);
        user.putAll(overrides);
        return user;
    }

    /**
     * Generates a list of mock users
     */
    public static List<Map<String, Object>> createMockUsers(int count) {
        List<Map<String, Object>> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(createMockUser("user-" + i));
        }
        return users;
    }

    /**
     * Generates a mock team member entity
     */
    public static Map<String, Object> createMockTeamMember(
            String userId,
            String teamId,
            String role) {
        Map<String, Object> member = new HashMap<>();
        member.put("userId", userId);
        member.put("teamId", teamId);
        member.put("role", role);
        member.put("joinedAt", Timestamp.valueOf(LocalDateTime.now()));
        return member;
    }

    /**
     * Generates a mock task board data structure
     */
    public static Map<String, Object> createMockTaskBoard(String teamId, int taskCount) {
        Map<String, Object> taskBoard = new HashMap<>();
        taskBoard.put("teamId", teamId);
        taskBoard.put("columns", createMockTaskColumns());
        taskBoard.put("tasks", createMockTasks(teamId, taskCount));
        taskBoard.put("users", createMockUsers(5));
        taskBoard.put("counts", createMockTaskCounts(taskCount));
        return taskBoard;
    }

    /**
     * Generates mock task columns
     */
    private static List<Map<String, Object>> createMockTaskColumns() {
        List<Map<String, Object>> columns = new ArrayList<>();
        
        Map<String, Object> todoColumn = new HashMap<>();
        todoColumn.put("status", "todo");
        todoColumn.put("title", "To Do");
        columns.add(todoColumn);
        
        Map<String, Object> inProgressColumn = new HashMap<>();
        inProgressColumn.put("status", "in_progress");
        inProgressColumn.put("title", "In Progress");
        columns.add(inProgressColumn);
        
        Map<String, Object> reviewColumn = new HashMap<>();
        reviewColumn.put("status", "review");
        reviewColumn.put("title", "Review");
        columns.add(reviewColumn);
        
        Map<String, Object> doneColumn = new HashMap<>();
        doneColumn.put("status", "done");
        doneColumn.put("title", "Done");
        columns.add(doneColumn);
        
        return columns;
    }

    /**
     * Generates mock task counts by status
     */
    private static Map<String, Integer> createMockTaskCounts(int totalTasks) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("todo", totalTasks / 4);
        counts.put("in_progress", totalTasks / 4);
        counts.put("review", totalTasks / 4);
        counts.put("done", totalTasks / 4);
        counts.put("total", totalTasks);
        return counts;
    }

    /**
     * Generates a mock query statistics object
     */
    public static Map<String, Object> createMockQueryStats(String queryName) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queryName", queryName);
        stats.put("count", 100L);
        stats.put("totalDuration", 5000L);
        stats.put("avgDuration", 50L);
        stats.put("p95Duration", 95L);
        stats.put("p99Duration", 150L);
        stats.put("maxDuration", 200L);
        return stats;
    }
}
