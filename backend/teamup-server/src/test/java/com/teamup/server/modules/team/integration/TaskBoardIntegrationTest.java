package com.teamup.server.modules.team.integration;

import com.teamup.server.modules.team.dto.TaskAssigneeDTO;
import com.teamup.server.modules.team.dto.TaskAttachmentDTO;
import com.teamup.server.modules.team.dto.TaskCommentDTO;
import com.teamup.server.modules.team.dto.TaskDetailDTO;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.entity.TaskAttachment;
import com.teamup.server.modules.team.entity.TaskComment;
import com.teamup.server.modules.team.service.TaskAssigneeService;
import com.teamup.server.modules.team.service.TaskAttachmentService;
import com.teamup.server.modules.team.service.TaskCommentService;
import com.teamup.server.modules.team.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务看板功能集成测试
 * 
 * 测试完整的任务生命周期，包括：
 * - 任务创建到删除的完整流程
 * - 任务分配、评论、附件的完整流程
 * - 权限控制在实际场景中的表现
 * 
 * Requirements: 所有需求
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskBoardIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskAssigneeService taskAssigneeService;

    @Autowired
    private TaskCommentService taskCommentService;

    @Autowired
    private TaskAttachmentService taskAttachmentService;

    private Long testTeamId;
    private Long testUserId;
    private Long testAssigneeId;

    @BeforeEach
    void setUp() {
        testTeamId = 1L;
        testUserId = 1L;
        testAssigneeId = 2L;
    }

    /**
     * 测试任务创建到删除的完整流程
     * 
     * 场景：
     * 1. 创建任务
     * 2. 验证任务已创建
     * 3. 更新任务状态
     * 4. 验证任务已更新
     * 5. 删除任务
     * 6. 验证任务已删除
     */
    @Test
    void testCompleteTaskLifecycle() {
        // 1. 创建任务
        Task task = createTestTask("Complete Lifecycle Task");
        Task createdTask = taskService.createTask(task);
        
        assertNotNull(createdTask.getId(), "Task should have an ID after creation");
        assertEquals("TODO", createdTask.getStatus(), "New task should have TODO status");
        assertNotNull(createdTask.getCreatedAt(), "Task should have creation timestamp");
        
        Long taskId = createdTask.getId();
        
        // 2. 验证任务已创建
        Task retrievedTask = taskService.getById(taskId);
        assertNotNull(retrievedTask, "Task should be retrievable after creation");
        assertEquals("Complete Lifecycle Task", retrievedTask.getTitle());
        
        // 3. 更新任务状态
        retrievedTask.setStatus("DOING");
        retrievedTask.setDescription("Updated description");
        Task updatedTask = taskService.updateTask(testUserId, retrievedTask);
        
        assertEquals("DOING", updatedTask.getStatus(), "Task status should be updated");
        assertEquals("Updated description", updatedTask.getDescription());
        assertTrue(updatedTask.getUpdatedAt().isAfter(updatedTask.getCreatedAt()), 
                   "Updated timestamp should be after created timestamp");
        
        // 4. 验证任务已更新
        Task verifyUpdated = taskService.getById(taskId);
        assertEquals("DOING", verifyUpdated.getStatus());
        
        // 5. 删除任务
        taskService.deleteTask(testUserId, taskId);
        
        // 6. 验证任务已删除
        Task deletedTask = taskService.getById(taskId);
        assertNull(deletedTask, "Task should be null after deletion");
    }

    /**
     * 测试任务分配、评论、附件的完整流程
     * 
     * 场景：
     * 1. 创建任务
     * 2. 添加负责人
     * 3. 添加评论
     * 4. 上传附件
     * 5. 获取任务详情，验证所有关联数据
     * 6. 删除评论
     * 7. 删除附件
     * 8. 移除负责人
     * 9. 删除任务，验证级联删除
     */
    @Test
    void testTaskWithAssigneesCommentsAndAttachments() {
        // 1. 创建任务
        Task task = createTestTask("Task with All Features");
        Task createdTask = taskService.createTask(task);
        Long taskId = createdTask.getId();
        
        // 2. 添加负责人
        TaskAssigneeDTO assignee1 = taskAssigneeService.addAssignee(taskId, testAssigneeId);
        TaskAssigneeDTO assignee2 = taskAssigneeService.addAssignee(taskId, testUserId);
        
        // 3. 添加评论
        TaskCommentDTO savedComment1 = taskCommentService.addComment(taskId, testUserId, "First comment");
        TaskCommentDTO savedComment2 = taskCommentService.addComment(taskId, testAssigneeId, "Second comment");
        
        // 4. 上传附件
        MockMultipartFile file1 = new MockMultipartFile(
            "file", "test1.txt", "text/plain", "Test content 1".getBytes()
        );
        TaskAttachmentDTO attachment1 = taskAttachmentService.uploadAttachment(taskId, file1, testUserId);
        
        MockMultipartFile file2 = new MockMultipartFile(
            "file", "test2.pdf", "application/pdf", "Test content 2".getBytes()
        );
        TaskAttachmentDTO attachment2 = taskAttachmentService.uploadAttachment(taskId, file2, testUserId);
        
        // 5. 获取任务详情，验证所有关联数据
        TaskDetailDTO taskDetail = taskService.getTaskDetail(taskId);
        
        assertNotNull(taskDetail, "Task detail should not be null");
        assertEquals("Task with All Features", taskDetail.getTitle());
        
        // 验证负责人
        List<TaskAssigneeDTO> assignees = taskDetail.getAssignees();
        assertEquals(2, assignees.size(), "Should have 2 assignees");
        assertTrue(assignees.stream().anyMatch(a -> a.getUserId().equals(testUserId)));
        assertTrue(assignees.stream().anyMatch(a -> a.getUserId().equals(testAssigneeId)));
        
        // 验证评论
        List<TaskCommentDTO> comments = taskDetail.getComments();
        assertEquals(2, comments.size(), "Should have 2 comments");
        assertEquals("First comment", comments.get(0).getContent());
        assertEquals("Second comment", comments.get(1).getContent());
        
        // 验证附件
        List<TaskAttachmentDTO> attachments = taskDetail.getAttachments();
        assertEquals(2, attachments.size(), "Should have 2 attachments");
        assertTrue(attachments.stream().anyMatch(a -> a.getFileName().equals("test1.txt")));
        assertTrue(attachments.stream().anyMatch(a -> a.getFileName().equals("test2.pdf")));
        
        // 6. 删除评论
        taskCommentService.deleteComment(testUserId, savedComment1.getId());
        List<TaskCommentDTO> remainingComments = taskCommentService.getCommentsByTaskId(taskId);
        assertEquals(1, remainingComments.size(), "Should have 1 comment after deletion");
        
        // 7. 删除附件
        taskAttachmentService.deleteAttachment(attachment1.getId());
        List<TaskAttachmentDTO> remainingAttachments = taskAttachmentService.getAttachmentsByTaskId(taskId);
        assertEquals(1, remainingAttachments.size(), "Should have 1 attachment after deletion");
        
        // 8. 移除负责人
        taskAssigneeService.removeAssignee(taskId, testAssigneeId);
        List<TaskAssigneeDTO> remainingAssignees = taskAssigneeService.getAssigneesByTaskId(taskId);
        assertEquals(1, remainingAssignees.size(), "Should have 1 assignee after removal");
        
        // 9. 删除任务，验证级联删除
        taskService.deleteTask(testUserId, taskId);
        
        // 验证任务已删除
        Task deletedTask = taskService.getById(taskId);
        assertNull(deletedTask, "Task should be deleted");
        
        // 验证关联数据已级联删除
        List<TaskAssigneeDTO> deletedAssignees = taskAssigneeService.getAssigneesByTaskId(taskId);
        assertTrue(deletedAssignees.isEmpty(), "Assignees should be cascade deleted");
        
        List<TaskCommentDTO> deletedComments = taskCommentService.getCommentsByTaskId(taskId);
        assertTrue(deletedComments.isEmpty(), "Comments should be cascade deleted");
        
        List<TaskAttachmentDTO> deletedAttachments = taskAttachmentService.getAttachmentsByTaskId(taskId);
        assertTrue(deletedAttachments.isEmpty(), "Attachments should be cascade deleted");
    }

    /**
     * 测试任务状态流转
     * 
     * 场景：
     * 1. 创建任务（TODO）
     * 2. 更新为DOING
     * 3. 更新为REVIEW
     * 4. 更新为DONE
     * 5. 验证每次状态变更都更新了时间戳
     */
    @Test
    void testTaskStatusTransitions() {
        // 1. 创建任务
        Task task = createTestTask("Status Transition Task");
        Task createdTask = taskService.createTask(task);
        Long taskId = createdTask.getId();
        
        assertEquals("TODO", createdTask.getStatus());
        LocalDateTime previousTimestamp = createdTask.getUpdatedAt();
        
        // 等待一小段时间确保时间戳不同
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 2. 更新为DOING
        Task taskForUpdate = taskService.getById(taskId);
        taskForUpdate.setStatus("DOING");
        Task doingTask = taskService.updateTask(testUserId, taskForUpdate);
        
        assertEquals("DOING", doingTask.getStatus());
        assertTrue(doingTask.getUpdatedAt().isAfter(previousTimestamp), 
                   "Timestamp should be updated when status changes");
        previousTimestamp = doingTask.getUpdatedAt();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 3. 更新为REVIEW
        taskForUpdate = taskService.getById(taskId);
        taskForUpdate.setStatus("REVIEW");
        Task reviewTask = taskService.updateTask(testUserId, taskForUpdate);
        
        assertEquals("REVIEW", reviewTask.getStatus());
        assertTrue(reviewTask.getUpdatedAt().isAfter(previousTimestamp));
        previousTimestamp = reviewTask.getUpdatedAt();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 4. 更新为DONE
        taskForUpdate = taskService.getById(taskId);
        taskForUpdate.setStatus("DONE");
        Task doneTask = taskService.updateTask(testUserId, taskForUpdate);
        
        assertEquals("DONE", doneTask.getStatus());
        assertTrue(doneTask.getUpdatedAt().isAfter(previousTimestamp));
    }

    /**
     * 测试多任务并发操作
     * 
     * 场景：
     * 1. 创建多个任务
     * 2. 为每个任务添加负责人、评论、附件
     * 3. 验证数据隔离性
     * 4. 批量删除任务
     */
    @Test
    void testMultipleTasksConcurrentOperations() {
        // 1. 创建多个任务
        Task task1 = createTestTask("Task 1");
        Task task2 = createTestTask("Task 2");
        Task task3 = createTestTask("Task 3");
        
        Task createdTask1 = taskService.createTask(task1);
        Task createdTask2 = taskService.createTask(task2);
        Task createdTask3 = taskService.createTask(task3);
        
        // 2. 为每个任务添加不同的数据
        // Task 1: 1 assignee, 2 comments, 1 attachment
        taskAssigneeService.addAssignee(createdTask1.getId(), testUserId);
        
        taskCommentService.addComment(createdTask1.getId(), testUserId, "Task 1 Comment 1");
        taskCommentService.addComment(createdTask1.getId(), testUserId, "Task 1 Comment 2");
        
        MockMultipartFile file1 = new MockMultipartFile(
            "file", "task1.txt", "text/plain", "Task 1 content".getBytes()
        );
        taskAttachmentService.uploadAttachment(createdTask1.getId(), file1, testUserId);
        
        // Task 2: 2 assignees, 1 comment, 2 attachments
        taskAssigneeService.addAssignee(createdTask2.getId(), testUserId);
        taskAssigneeService.addAssignee(createdTask2.getId(), testAssigneeId);
        
        taskCommentService.addComment(createdTask2.getId(), testUserId, "Task 2 Comment");
        
        MockMultipartFile file2_1 = new MockMultipartFile(
            "file", "task2_1.txt", "text/plain", "Task 2 content 1".getBytes()
        );
        taskAttachmentService.uploadAttachment(createdTask2.getId(), file2_1, testUserId);
        
        MockMultipartFile file2_2 = new MockMultipartFile(
            "file", "task2_2.txt", "text/plain", "Task 2 content 2".getBytes()
        );
        taskAttachmentService.uploadAttachment(createdTask2.getId(), file2_2, testUserId);
        
        // Task 3: 0 assignees, 0 comments, 0 attachments (minimal task)
        
        // 3. 验证数据隔离性
        TaskDetailDTO detail1 = taskService.getTaskDetail(createdTask1.getId());
        assertEquals(1, detail1.getAssignees().size());
        assertEquals(2, detail1.getComments().size());
        assertEquals(1, detail1.getAttachments().size());
        
        TaskDetailDTO detail2 = taskService.getTaskDetail(createdTask2.getId());
        assertEquals(2, detail2.getAssignees().size());
        assertEquals(1, detail2.getComments().size());
        assertEquals(2, detail2.getAttachments().size());
        
        TaskDetailDTO detail3 = taskService.getTaskDetail(createdTask3.getId());
        assertEquals(0, detail3.getAssignees().size());
        assertEquals(0, detail3.getComments().size());
        assertEquals(0, detail3.getAttachments().size());
        
        // 4. 批量删除任务
        taskService.deleteTask(testUserId, createdTask1.getId());
        taskService.deleteTask(testUserId, createdTask2.getId());
        taskService.deleteTask(testUserId, createdTask3.getId());
        
        // 验证所有任务已删除
        assertNull(taskService.getById(createdTask1.getId()));
        assertNull(taskService.getById(createdTask2.getId()));
        assertNull(taskService.getById(createdTask3.getId()));
    }

    /**
     * 辅助方法：创建测试任务
     */
    private Task createTestTask(String title) {
        Task task = new Task();
        task.setTeamId(testTeamId);
        task.setTitle(title);
        task.setDescription("Test description for " + title);
        task.setStatus("TODO");
        task.setPriority("MEDIUM");
        task.setDeadline(LocalDate.now().plusDays(7));
        task.setCreatedBy(testUserId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
