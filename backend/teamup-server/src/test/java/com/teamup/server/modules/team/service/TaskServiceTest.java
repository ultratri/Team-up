package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.team.dto.TaskFilterDTO;
import com.teamup.server.modules.team.dto.TaskStatsDTO;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 任务服务单元测试
 * Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2
 */
class TaskServiceTest {

    private TaskService taskService;
    private TaskMapper taskMapper;
    private TaskAssigneeService taskAssigneeService;
    private List<Task> testTasks;

    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        taskAssigneeService = mock(TaskAssigneeService.class);
        TaskServiceImpl taskServiceImpl = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskServiceImpl, "baseMapper", taskMapper);
        ReflectionTestUtils.setField(taskServiceImpl, "taskAssigneeService", taskAssigneeService);
        taskService = taskServiceImpl;
        
        // Create test data
        testTasks = new ArrayList<>();
        testTasks.add(createTask(1L, 1L, "TODO", "HIGH", "Task 1"));
        testTasks.add(createTask(2L, 1L, "DOING", "MEDIUM", "Task 2"));
        testTasks.add(createTask(3L, 1L, "REVIEW", "LOW", "Task 3"));
        testTasks.add(createTask(4L, 1L, "DONE", "HIGH", "Task 4"));
        testTasks.add(createTask(5L, 1L, "TODO", "MEDIUM", "Important Task"));
    }

    /**
     * 测试按状态筛选
     * Requirements: 5.1
     */
    @Test
    void testFilterByStatus() {
        // Given
        Long teamId = 1L;
        String status = "TODO";
        
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return testTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && t.getStatus().equals(status))
                .collect(Collectors.toList());
        });
        
        // When
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setStatus(status);
        List<Task> result = taskService.filterTasks(teamId, filter);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getStatus().equals(status)));
    }

    /**
     * 测试按优先级筛选
     * Requirements: 5.2
     */
    @Test
    void testFilterByPriority() {
        // Given
        Long teamId = 1L;
        String priority = "HIGH";
        
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return testTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && t.getPriority().equals(priority))
                .collect(Collectors.toList());
        });
        
        // When
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setPriority(priority);
        List<Task> result = taskService.filterTasks(teamId, filter);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getPriority().equals(priority)));
    }

    /**
     * 测试按负责人筛选
     * Requirements: 5.3
     */
    @Test
    void testFilterByAssignee() {
        // Given
        Long teamId = 1L;
        Long assigneeId = 100L;
        List<Long> assignedTaskIds = List.of(1L, 3L);
        
        when(taskAssigneeService.getTaskIdsByUserId(assigneeId)).thenReturn(assignedTaskIds);
        
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return testTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && assignedTaskIds.contains(t.getId()))
                .collect(Collectors.toList());
        });
        
        // When
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setAssigneeId(assigneeId);
        List<Task> result = taskService.filterTasks(teamId, filter);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> assignedTaskIds.contains(t.getId())));
    }

    /**
     * 测试关键词搜索
     * Requirements: 5.4
     */
    @Test
    void testSearchByKeyword() {
        // Given
        Long teamId = 1L;
        String keyword = "Important";
        
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return testTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && 
                            t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        });
        
        // When
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setKeyword(keyword);
        List<Task> result = taskService.filterTasks(teamId, filter);
        
        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTitle().contains(keyword));
    }

    /**
     * 测试多条件组合筛选
     * Requirements: 5.1, 5.2
     */
    @Test
    void testFilterByMultipleCriteria() {
        // Given
        Long teamId = 1L;
        String status = "TODO";
        String priority = "HIGH";
        
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return testTasks.stream()
                .filter(t -> t.getTeamId().equals(teamId) && 
                            t.getStatus().equals(status) && 
                            t.getPriority().equals(priority))
                .collect(Collectors.toList());
        });
        
        // When
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setStatus(status);
        filter.setPriority(priority);
        List<Task> result = taskService.filterTasks(teamId, filter);
        
        // Then
        assertEquals(1, result.size());
        assertEquals(status, result.get(0).getStatus());
        assertEquals(priority, result.get(0).getPriority());
    }

    /**
     * 测试统计计算正确性
     * Requirements: 6.1, 6.2
     */
    @Test
    void testTaskStatsCalculation() {
        // Given
        Long teamId = 1L;
        
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(testTasks);
        
        // When
        TaskStatsDTO stats = taskService.getTaskStats(teamId);
        
        // Then
        assertEquals(5, stats.getTotalCount());
        assertEquals(2, stats.getTodoCount());
        assertEquals(1, stats.getDoingCount());
        assertEquals(1, stats.getReviewCount());
        assertEquals(1, stats.getDoneCount());
        assertEquals(20.0, stats.getCompletionRate(), 0.01); // 1/5 * 100 = 20%
    }

    /**
     * 测试逾期任务统计
     * Requirements: 6.1, 6.2
     */
    @Test
    void testOverdueTasksCount() {
        // Given
        Long teamId = 1L;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Set deadlines
        testTasks.get(0).setDeadline(yesterday); // TODO - overdue
        testTasks.get(1).setDeadline(yesterday); // DOING - overdue
        testTasks.get(3).setDeadline(yesterday); // DONE - not overdue
        testTasks.get(4).setDeadline(LocalDate.now().plusDays(1)); // TODO - not overdue
        
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(testTasks);
        
        // When
        TaskStatsDTO stats = taskService.getTaskStats(teamId);
        
        // Then
        assertEquals(2, stats.getOverdueCount());
    }

    /**
     * 测试空结果筛选
     * Requirements: 5.1
     */
    @Test
    void testFilterReturnsEmptyWhenNoMatch() {
        // Given
        Long teamId = 1L;
        String status = "ARCHIVED";
        
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(new ArrayList<>());
        
        // When
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setStatus(status);
        List<Task> result = taskService.filterTasks(teamId, filter);
        
        // Then
        assertTrue(result.isEmpty());
    }

    /**
     * 测试完成率为0的情况
     * Requirements: 6.2
     */
    @Test
    void testCompletionRateWhenNoTasks() {
        // Given
        Long teamId = 1L;
        
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(new ArrayList<>());
        
        // When
        TaskStatsDTO stats = taskService.getTaskStats(teamId);
        
        // Then
        assertEquals(0, stats.getTotalCount());
        assertEquals(0.0, stats.getCompletionRate());
    }

    // Helper method
    private Task createTask(Long id, Long teamId, String status, String priority, String title) {
        Task task = new Task();
        task.setId(id);
        task.setTeamId(teamId);
        task.setTitle(title);
        task.setDescription("Test description");
        task.setStatus(status);
        task.setPriority(priority);
        task.setCreatedBy(1L);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
