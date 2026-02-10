package com.teamup.server.modules.team.integration;

import com.teamup.server.modules.team.dto.TaskFilterDTO;
import com.teamup.server.modules.team.dto.TaskStatsDTO;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.service.TaskAssigneeService;
import com.teamup.server.modules.team.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务筛选和统计功能集成测试
 * 
 * 测试筛选和统计功能在实际场景中的表现：
 * - 按状态筛选
 * - 按优先级筛选
 * - 按负责人筛选
 * - 关键词搜索
 * - 多条件组合筛选
 * - 任务统计计算
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskFilterAndStatsIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskAssigneeService taskAssigneeService;

    private Long testTeamId;
    private Long testUserId1;
    private Long testUserId2;

    @BeforeEach
    void setUp() {
        testTeamId = 1L;
        testUserId1 = 1L;
        testUserId2 = 2L;
    }

    /**
     * 测试按状态筛选任务
     */
    @Test
    void testFilterByStatus() {
        // 创建不同状态的任务
        createTestTask("TODO Task 1", "TODO", "HIGH");
        createTestTask("TODO Task 2", "TODO", "MEDIUM");
        createTestTask("DOING Task 1", "DOING", "HIGH");
        createTestTask("REVIEW Task 1", "REVIEW", "LOW");
        createTestTask("DONE Task 1", "DONE", "MEDIUM");
        
        // 筛选TODO状态的任务
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setStatus("TODO");
        List<Task> todoTasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(2, todoTasks.size(), "Should have 2 TODO tasks");
        assertTrue(todoTasks.stream().allMatch(t -> "TODO".equals(t.getStatus())),
                   "All filtered tasks should have TODO status");
        
        // 筛选DONE状态的任务
        filter.setStatus("DONE");
        List<Task> doneTasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(1, doneTasks.size(), "Should have 1 DONE task");
        assertEquals("DONE", doneTasks.get(0).getStatus());
    }

    /**
     * 测试按优先级筛选任务
     */
    @Test
    void testFilterByPriority() {
        // 创建不同优先级的任务
        createTestTask("High Priority 1", "TODO", "HIGH");
        createTestTask("High Priority 2", "DOING", "HIGH");
        createTestTask("Medium Priority 1", "TODO", "MEDIUM");
        createTestTask("Low Priority 1", "REVIEW", "LOW");
        
        // 筛选HIGH优先级的任务
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setPriority("HIGH");
        List<Task> highPriorityTasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(2, highPriorityTasks.size(), "Should have 2 HIGH priority tasks");
        assertTrue(highPriorityTasks.stream().allMatch(t -> "HIGH".equals(t.getPriority())),
                   "All filtered tasks should have HIGH priority");
    }

    /**
     * 测试按负责人筛选任务
     */
    @Test
    void testFilterByAssignee() {
        // 创建任务并分配负责人
        Task task1 = createTestTask("Task for User 1", "TODO", "HIGH");
        Task task2 = createTestTask("Task for User 2", "DOING", "MEDIUM");
        Task task3 = createTestTask("Task for Both", "TODO", "LOW");
        Task task4 = createTestTask("Task Unassigned", "REVIEW", "MEDIUM");
        
        // 分配负责人
        addAssignee(task1.getId(), testUserId1);
        addAssignee(task2.getId(), testUserId2);
        addAssignee(task3.getId(), testUserId1);
        addAssignee(task3.getId(), testUserId2);
        
        // 筛选分配给testUserId1的任务
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setAssigneeId(testUserId1);
        List<Task> user1Tasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(2, user1Tasks.size(), "User 1 should have 2 assigned tasks");
        
        // 筛选分配给testUserId2的任务
        filter.setAssigneeId(testUserId2);
        List<Task> user2Tasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(2, user2Tasks.size(), "User 2 should have 2 assigned tasks");
    }

    /**
     * 测试关键词搜索
     */
    @Test
    void testSearchByKeyword() {
        // 创建包含不同关键词的任务
        createTestTask("Implement user authentication", "TODO", "HIGH");
        createTestTask("Design database schema", "DOING", "MEDIUM");
        createTestTask("Write user documentation", "REVIEW", "LOW");
        createTestTask("Fix login bug", "TODO", "HIGH");
        
        // 搜索包含"user"的任务
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setKeyword("user");
        List<Task> userTasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(2, userTasks.size(), "Should find 2 tasks containing 'user'");
        assertTrue(userTasks.stream().allMatch(t -> 
            t.getTitle().toLowerCase().contains("user")),
            "All tasks should contain 'user' in title");
        
        // 搜索包含"database"的任务
        filter.setKeyword("database");
        List<Task> databaseTasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(1, databaseTasks.size(), "Should find 1 task containing 'database'");
    }

    /**
     * 测试多条件组合筛选
     */
    @Test
    void testCombinedFilters() {
        // 创建多个任务
        Task task1 = createTestTask("High priority TODO task", "TODO", "HIGH");
        Task task2 = createTestTask("High priority DOING task", "DOING", "HIGH");
        Task task3 = createTestTask("Medium priority TODO task", "TODO", "MEDIUM");
        Task task4 = createTestTask("High priority TODO for user", "TODO", "HIGH");
        
        // 分配负责人
        addAssignee(task1.getId(), testUserId1);
        addAssignee(task4.getId(), testUserId1);
        
        // 组合筛选：状态=TODO AND 优先级=HIGH
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setStatus("TODO");
        filter.setPriority("HIGH");
        List<Task> filtered1 = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(2, filtered1.size(), "Should find 2 tasks with TODO status and HIGH priority");
        assertTrue(filtered1.stream().allMatch(t -> 
            "TODO".equals(t.getStatus()) && "HIGH".equals(t.getPriority())),
            "All tasks should match both criteria");
        
        // 组合筛选：状态=TODO AND 优先级=HIGH AND 负责人=testUserId1
        filter.setAssigneeId(testUserId1);
        List<Task> filtered2 = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(2, filtered2.size(), 
            "Should find 2 tasks with TODO status, HIGH priority, and assigned to user 1");
        
        // 组合筛选：关键词="user" AND 状态=TODO
        filter = new TaskFilterDTO();
        filter.setKeyword("user");
        filter.setStatus("TODO");
        List<Task> filtered3 = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(1, filtered3.size(), 
            "Should find 1 task containing 'user' with TODO status");
    }

    /**
     * 测试任务统计计算
     */
    @Test
    void testTaskStatistics() {
        // 创建不同状态的任务
        createTestTask("TODO 1", "TODO", "HIGH");
        createTestTask("TODO 2", "TODO", "MEDIUM");
        createTestTask("TODO 3", "TODO", "LOW");
        createTestTask("DOING 1", "DOING", "HIGH");
        createTestTask("DOING 2", "DOING", "MEDIUM");
        createTestTask("REVIEW 1", "REVIEW", "HIGH");
        createTestTask("DONE 1", "DONE", "MEDIUM");
        createTestTask("DONE 2", "DONE", "LOW");
        
        // 获取统计信息
        TaskStatsDTO stats = taskService.getTaskStats(testTeamId);
        
        assertNotNull(stats, "Stats should not be null");
        assertEquals(3, stats.getTodoCount(), "Should have 3 TODO tasks");
        assertEquals(2, stats.getDoingCount(), "Should have 2 DOING tasks");
        assertEquals(1, stats.getReviewCount(), "Should have 1 REVIEW task");
        assertEquals(2, stats.getDoneCount(), "Should have 2 DONE tasks");
        assertEquals(8, stats.getTotalCount(), "Should have 8 total tasks");
        
        // 验证完成率计算
        double expectedCompletionRate = (2.0 / 8.0) * 100;
        assertEquals(expectedCompletionRate, stats.getCompletionRate(), 0.01,
                     "Completion rate should be 25%");
    }

    /**
     * 测试逾期任务统计
     */
    @Test
    void testOverdueTaskStatistics() {
        // 创建逾期任务（截止日期在过去）
        Task overdueTask1 = new Task();
        overdueTask1.setTeamId(testTeamId);
        overdueTask1.setTitle("Overdue Task 1");
        overdueTask1.setStatus("TODO");
        overdueTask1.setPriority("HIGH");
        overdueTask1.setDeadline(LocalDate.now().minusDays(5));
        overdueTask1.setCreatedBy(testUserId1);
        overdueTask1.setCreatedAt(LocalDateTime.now());
        overdueTask1.setUpdatedAt(LocalDateTime.now());
        taskService.createTask(overdueTask1);
        
        Task overdueTask2 = new Task();
        overdueTask2.setTeamId(testTeamId);
        overdueTask2.setTitle("Overdue Task 2");
        overdueTask2.setStatus("DOING");
        overdueTask2.setPriority("MEDIUM");
        overdueTask2.setDeadline(LocalDate.now().minusDays(2));
        overdueTask2.setCreatedBy(testUserId1);
        overdueTask2.setCreatedAt(LocalDateTime.now());
        overdueTask2.setUpdatedAt(LocalDateTime.now());
        taskService.createTask(overdueTask2);
        
        // 创建未逾期任务
        createTestTask("Not Overdue", "TODO", "LOW");
        
        // 创建已完成的逾期任务（不应计入逾期）
        Task completedOverdue = new Task();
        completedOverdue.setTeamId(testTeamId);
        completedOverdue.setTitle("Completed Overdue");
        completedOverdue.setStatus("DONE");
        completedOverdue.setPriority("HIGH");
        completedOverdue.setDeadline(LocalDate.now().minusDays(3));
        completedOverdue.setCreatedBy(testUserId1);
        completedOverdue.setCreatedAt(LocalDateTime.now());
        completedOverdue.setUpdatedAt(LocalDateTime.now());
        taskService.createTask(completedOverdue);
        
        // 获取统计信息
        TaskStatsDTO stats = taskService.getTaskStats(testTeamId);
        
        assertEquals(2, stats.getOverdueCount(), 
            "Should have 2 overdue tasks (excluding completed ones)");
    }

    /**
     * 测试空筛选条件返回所有任务
     */
    @Test
    void testEmptyFilterReturnsAllTasks() {
        // 创建多个任务
        createTestTask("Task 1", "TODO", "HIGH");
        createTestTask("Task 2", "DOING", "MEDIUM");
        createTestTask("Task 3", "REVIEW", "LOW");
        
        // 空筛选条件
        TaskFilterDTO filter = new TaskFilterDTO();
        List<Task> allTasks = taskService.filterTasks(testTeamId, filter);
        
        assertEquals(3, allTasks.size(), "Empty filter should return all tasks");
    }

    /**
     * 测试筛选结果为空的情况
     */
    @Test
    void testFilterReturnsEmptyResult() {
        // 创建一些任务
        createTestTask("Task 1", "TODO", "HIGH");
        createTestTask("Task 2", "DOING", "MEDIUM");
        
        // 筛选不存在的状态
        TaskFilterDTO filter = new TaskFilterDTO();
        filter.setStatus("DONE");
        List<Task> doneTasks = taskService.filterTasks(testTeamId, filter);
        
        assertTrue(doneTasks.isEmpty(), "Should return empty list when no tasks match");
        
        // 搜索不存在的关键词
        filter = new TaskFilterDTO();
        filter.setKeyword("nonexistent");
        List<Task> searchResults = taskService.filterTasks(testTeamId, filter);
        
        assertTrue(searchResults.isEmpty(), "Should return empty list when no tasks match keyword");
    }

    /**
     * 测试统计信息在任务变化时的实时更新
     */
    @Test
    void testStatsUpdateWhenTasksChange() {
        // 初始状态：创建一些任务
        Task task1 = createTestTask("Task 1", "TODO", "HIGH");
        Task task2 = createTestTask("Task 2", "TODO", "MEDIUM");
        
        TaskStatsDTO initialStats = taskService.getTaskStats(testTeamId);
        assertEquals(2, initialStats.getTodoCount());
        assertEquals(0, initialStats.getDoneCount());
        assertEquals(0.0, initialStats.getCompletionRate());
        
        // 更新任务状态
        task1.setStatus("DONE");
        taskService.updateTask(testUserId1, task1);
        
        TaskStatsDTO updatedStats = taskService.getTaskStats(testTeamId);
        assertEquals(1, updatedStats.getTodoCount(), "TODO count should decrease");
        assertEquals(1, updatedStats.getDoneCount(), "DONE count should increase");
        assertEquals(50.0, updatedStats.getCompletionRate(), 0.01, 
                     "Completion rate should be 50%");
        
        // 删除一个任务
        taskService.deleteTask(testUserId1, task2.getId());
        
        TaskStatsDTO finalStats = taskService.getTaskStats(testTeamId);
        assertEquals(0, finalStats.getTodoCount());
        assertEquals(1, finalStats.getDoneCount());
        assertEquals(1, finalStats.getTotalCount());
        assertEquals(100.0, finalStats.getCompletionRate(), 0.01,
                     "Completion rate should be 100%");
    }

    /**
     * 辅助方法：创建测试任务
     */
    private Task createTestTask(String title, String status, String priority) {
        Task task = new Task();
        task.setTeamId(testTeamId);
        task.setTitle(title);
        task.setDescription("Test description");
        task.setStatus(status);
        task.setPriority(priority);
        task.setDeadline(LocalDate.now().plusDays(7));
        task.setCreatedBy(testUserId1);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskService.createTask(task);
    }

    /**
     * 辅助方法：添加负责人
     */
    private void addAssignee(Long taskId, Long userId) {
        taskAssigneeService.addAssignee(taskId, userId);
    }
}
