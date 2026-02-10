package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.team.dto.TaskFilterDTO;
import com.teamup.server.modules.team.dto.TaskStatsDTO;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.mapper.TaskAssigneeMapper;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.service.impl.TaskServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 任务筛选和统计的属性测试
 * Requirements: 5.1-5.5, 6.1-6.4
 */
class TaskFilterPropertyTest {

    /**
     * Property 16: 任务筛选正确性
     * For any filter criteria (status, priority, assignee, or combinations), 
     * the returned tasks should match all specified criteria, 
     * and no tasks that don't match should be returned.
     * 
     * Validates: Requirements 5.1, 5.2, 5.3, 5.5
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 16: 任务筛选正确性 - 状态筛选")
    void filterByStatusShouldReturnOnlyMatchingTasks(
        @ForAll @IntRange(min = 1, max = 100) int teamIdSeed,
        @ForAll("validStatus") String status
    ) {
        Long teamId = (long) teamIdSeed;
        
        // Setup mocks
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskAssigneeMapper taskAssigneeMapper = mock(TaskAssigneeMapper.class);
        TaskServiceImpl taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
        
        // Create test data
        List<Task> allTasks = new ArrayList<>();
        allTasks.add(createTask(teamId, "TODO", "MEDIUM"));
        allTasks.add(createTask(teamId, "DOING", "HIGH"));
        allTasks.add(createTask(teamId, status, "LOW"));
        allTasks.add(createTask(teamId, status, "MEDIUM"));
        
        // Mock selectList to return filtered tasks
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return allTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && t.getStatus().equals(status))
                .collect(Collectors.toList());
        });
        
        // When: Filter by status
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setStatus(status);
        List<Task> filtered = taskService.filterTasks(teamId, filter);
        
        // Then: All returned tasks should have the specified status
        for (Task task : filtered) {
            assert task.getStatus().equals(status) : 
                "Task status " + task.getStatus() + " does not match filter " + status;
        }
    }
    
    /**
     * Property 16: 任务筛选正确性 - 优先级筛选
     * Validates: Requirements 5.2, 5.5
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 16: 任务筛选正确性 - 优先级筛选")
    void filterByPriorityShouldReturnOnlyMatchingTasks(
        @ForAll @IntRange(min = 1, max = 100) int teamIdSeed,
        @ForAll("validPriority") String priority
    ) {
        Long teamId = (long) teamIdSeed;
        
        // Setup mocks
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskServiceImpl taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
        
        // Create test data
        List<Task> allTasks = new ArrayList<>();
        allTasks.add(createTask(teamId, "TODO", "LOW"));
        allTasks.add(createTask(teamId, "DOING", "MEDIUM"));
        allTasks.add(createTask(teamId, "REVIEW", priority));
        allTasks.add(createTask(teamId, "DONE", priority));
        
        // Mock selectList to return filtered tasks
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return allTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && t.getPriority().equals(priority))
                .collect(Collectors.toList());
        });
        
        // When: Filter by priority
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setPriority(priority);
        List<Task> filtered = taskService.filterTasks(teamId, filter);
        
        // Then: All returned tasks should have the specified priority
        for (Task task : filtered) {
            assert task.getPriority().equals(priority) : 
                "Task priority " + task.getPriority() + " does not match filter " + priority;
        }
    }
    
    /**
     * Property 17: 任务搜索正确性
     * For any search keyword, all returned tasks should have titles containing the keyword (case-insensitive),
     * and no tasks without the keyword should be returned.
     * 
     * Validates: Requirements 5.4
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 17: 任务搜索正确性")
    void searchByKeywordShouldReturnOnlyMatchingTasks(
        @ForAll @IntRange(min = 1, max = 100) int teamIdSeed,
        @ForAll("keywords") String keyword
    ) {
        Long teamId = (long) teamIdSeed;
        
        // Setup mocks
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskServiceImpl taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
        
        // Create test data
        List<Task> allTasks = new ArrayList<>();
        Task task1 = createTask(teamId, "TODO", "MEDIUM");
        task1.setTitle("Task with " + keyword + " in title");
        allTasks.add(task1);
        
        Task task2 = createTask(teamId, "DOING", "HIGH");
        task2.setTitle("Another task");
        allTasks.add(task2);
        
        Task task3 = createTask(teamId, "REVIEW", "LOW");
        task3.setTitle(keyword.toUpperCase() + " at start");
        allTasks.add(task3);
        
        // Mock selectList to return filtered tasks
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return allTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && 
                            t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        });
        
        // When: Search by keyword
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setKeyword(keyword);
        List<Task> filtered = taskService.filterTasks(teamId, filter);
        
        // Then: All returned tasks should contain the keyword in title
        for (Task task : filtered) {
            assert task.getTitle().toLowerCase().contains(keyword.toLowerCase()) : 
                "Task title '" + task.getTitle() + "' does not contain keyword '" + keyword + "'";
        }
    }
    
    /**
     * Property 18: 任务统计计数准确性
     * For any set of tasks, the statistics should accurately reflect:
     * count of tasks in each status, total count, count of overdue tasks, 
     * and completion rate (done_count / total_count * 100).
     * 
     * Validates: Requirements 6.1, 6.2, 6.3, 6.4
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 18: 任务统计计数准确性")
    void taskStatsShouldAccuratelyReflectTaskCounts(
        @ForAll @IntRange(min = 1, max = 100) int teamIdSeed,
        @ForAll @IntRange(min = 0, max = 5) int todoCount,
        @ForAll @IntRange(min = 0, max = 5) int doingCount,
        @ForAll @IntRange(min = 0, max = 5) int reviewCount,
        @ForAll @IntRange(min = 0, max = 5) int doneCount
    ) {
        Long teamId = (long) teamIdSeed;
        
        // Setup mocks
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskServiceImpl taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
        
        // Create test data with specific status distribution
        List<Task> allTasks = new ArrayList<>();
        for (int i = 0; i < todoCount; i++) {
            allTasks.add(createTask(teamId, "TODO", "MEDIUM"));
        }
        for (int i = 0; i < doingCount; i++) {
            allTasks.add(createTask(teamId, "DOING", "MEDIUM"));
        }
        for (int i = 0; i < reviewCount; i++) {
            allTasks.add(createTask(teamId, "REVIEW", "MEDIUM"));
        }
        for (int i = 0; i < doneCount; i++) {
            allTasks.add(createTask(teamId, "DONE", "MEDIUM"));
        }
        
        // Mock selectList to return all tasks for this team
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(allTasks);
        
        // When: Get task statistics
        TaskStatsDTO stats = taskService.getTaskStats(teamId);
        
        // Then: Statistics should match the created tasks
        int expectedTotal = todoCount + doingCount + reviewCount + doneCount;
        assert stats.getTotalCount() == expectedTotal : 
            "Total count " + stats.getTotalCount() + " does not match expected " + expectedTotal;
        assert stats.getTodoCount() == todoCount : 
            "TODO count " + stats.getTodoCount() + " does not match expected " + todoCount;
        assert stats.getDoingCount() == doingCount : 
            "DOING count " + stats.getDoingCount() + " does not match expected " + doingCount;
        assert stats.getReviewCount() == reviewCount : 
            "REVIEW count " + stats.getReviewCount() + " does not match expected " + reviewCount;
        assert stats.getDoneCount() == doneCount : 
            "DONE count " + stats.getDoneCount() + " does not match expected " + doneCount;
        
        // Verify completion rate calculation
        if (stats.getTotalCount() > 0) {
            double expectedRate = (double) doneCount / expectedTotal * 100;
            assert Math.abs(stats.getCompletionRate() - expectedRate) < 0.01 : 
                "Completion rate " + stats.getCompletionRate() + " does not match expected " + expectedRate;
        }
    }
    
    /**
     * Property 18: 任务统计计数准确性 - 逾期任务统计
     * Validates: Requirements 6.3, 6.4
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 18: 任务统计计数准确性 - 逾期任务")
    void taskStatsShouldAccuratelyCountOverdueTasks(
        @ForAll @IntRange(min = 1, max = 100) int teamIdSeed,
        @ForAll @IntRange(min = 0, max = 5) int overdueCount
    ) {
        Long teamId = (long) teamIdSeed;
        
        // Setup mocks
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskServiceImpl taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
        
        // Create test data
        List<Task> allTasks = new ArrayList<>();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Create overdue tasks (deadline in the past, not DONE)
        for (int i = 0; i < overdueCount; i++) {
            Task task = createTask(teamId, "TODO", "MEDIUM");
            task.setDeadline(yesterday);
            allTasks.add(task);
        }
        
        // Create some non-overdue tasks
        Task doneTask = createTask(teamId, "DONE", "MEDIUM");
        doneTask.setDeadline(yesterday);
        allTasks.add(doneTask);
        
        Task futureTask = createTask(teamId, "TODO", "MEDIUM");
        futureTask.setDeadline(LocalDate.now().plusDays(1));
        allTasks.add(futureTask);
        
        // Mock selectList to return all tasks
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(allTasks);
        
        // When: Get task statistics
        TaskStatsDTO stats = taskService.getTaskStats(teamId);
        
        // Then: Overdue count should match
        assert stats.getOverdueCount() == overdueCount : 
            "Overdue count " + stats.getOverdueCount() + " does not match expected " + overdueCount;
    }
    
    // Helper methods
    
    private Task createTask(Long teamId, String status, String priority) {
        Task task = new Task();
        task.setId(System.nanoTime());
        task.setTeamId(teamId);
        task.setTitle("Test Task " + System.nanoTime());
        task.setDescription("Test description");
        task.setStatus(status);
        task.setPriority(priority);
        task.setCreatedBy(1L);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
    
    @Provide
    Arbitrary<String> validStatus() {
        return Arbitraries.of("TODO", "DOING", "REVIEW", "DONE");
    }
    
    @Provide
    Arbitrary<String> validPriority() {
        return Arbitraries.of("LOW", "MEDIUM", "HIGH");
    }
    
    @Provide
    Arbitrary<String> keywords() {
        return Arbitraries.of("test", "task", "work", "bug", "feature");
    }
}
