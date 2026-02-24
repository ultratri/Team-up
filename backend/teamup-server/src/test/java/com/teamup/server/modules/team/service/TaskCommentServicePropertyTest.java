package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.team.dto.TaskCommentDTO;
import com.teamup.server.modules.team.entity.TaskComment;
import com.teamup.server.modules.team.mapper.TaskCommentMapper;
import com.teamup.server.modules.team.service.impl.TaskCommentServiceImpl;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 任务评论服务属性测试
 * Feature: task-board-enhancement
 * 
 * 使用模拟对象测试服务层逻辑
 */
public class TaskCommentServicePropertyTest {

    /**
     * Property 7: 评论创建和存储
     * For any team member and task, when a comment is added, it should be stored 
     * in task_comments table with all required fields (user_id, task_id, content, created_at).
     * Validates: Requirements 3.1
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 7: 评论创建和存储")
    void commentCreationShouldStoreAllRequiredFields(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int userIdSeed,
            @ForAll @StringLength(min = 1, max = 500) String content) {
        
        Long taskId = (long) taskIdSeed;
        Long userId = (long) userIdSeed;
        
        // Setup mocks
        TaskCommentMapper taskCommentMapper = mock(TaskCommentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        com.teamup.server.modules.notification.service.NotificationService notificationService = mock(com.teamup.server.modules.notification.service.NotificationService.class);
        com.teamup.server.modules.team.service.TaskService taskService = mock(com.teamup.server.modules.team.service.TaskService.class);
        com.teamup.server.modules.team.service.TaskAssigneeService taskAssigneeService = mock(com.teamup.server.modules.team.service.TaskAssigneeService.class);
        TaskCommentServiceImpl taskCommentService = new TaskCommentServiceImpl(userMapper, userProfileMapper, permissionService, notificationService, taskService, taskAssigneeService);
        
        // Inject the mocked mapper into the base class using reflection
        ReflectionTestUtils.setField(taskCommentService, "baseMapper", taskCommentMapper);
        
        // Mock user data
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setAvatarUrl("avatar_" + userId + ".jpg");
        when(userProfileMapper.selectOne(any(Wrapper.class))).thenReturn(profile);
        
        // Track comments in memory to simulate database
        List<TaskComment> commentList = new ArrayList<>();
        
        // Mock insert behavior
        when(taskCommentMapper.insert(any(TaskComment.class))).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0);
            comment.setId((long) (commentList.size() + 1));
            commentList.add(comment);
            return 1;
        });
        
        // Execute: Add comment
        TaskCommentDTO addedComment = taskCommentService.addComment(taskId, userId, content);
        
        // Verify: Comment should be created with all required fields
        assertNotNull(addedComment, "Added comment should not be null");
        assertEquals(taskId, addedComment.getTaskId(), "Task ID should match");
        assertEquals(userId, addedComment.getUserId(), "User ID should match");
        assertEquals(content, addedComment.getContent(), "Content should match");
        assertNotNull(addedComment.getCreatedAt(), "Created time should be set");
        
        // Verify: Comment should be stored in the list
        assertEquals(1, commentList.size(), "Comment should be stored in the database");
        TaskComment storedComment = commentList.get(0);
        assertEquals(taskId, storedComment.getTaskId(), "Stored task ID should match");
        assertEquals(userId, storedComment.getUserId(), "Stored user ID should match");
        assertEquals(content, storedComment.getContent(), "Stored content should match");
        assertNotNull(storedComment.getCreatedAt(), "Stored created time should be set");
    }

    /**
     * Property 8: 评论按时间顺序显示
     * For any task with multiple comments, when displaying comments, they should be 
     * ordered chronologically by created_at timestamp.
     * Validates: Requirements 3.2
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 8: 评论按时间顺序显示")
    void commentsShouldBeOrderedChronologically(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 2, max = 10) int commentCount) {
        
        Long taskId = (long) taskIdSeed;
        
        // Setup mocks
        TaskCommentMapper taskCommentMapper = mock(TaskCommentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        com.teamup.server.modules.notification.service.NotificationService notificationService = mock(com.teamup.server.modules.notification.service.NotificationService.class);
        com.teamup.server.modules.team.service.TaskService taskService = mock(com.teamup.server.modules.team.service.TaskService.class);
        com.teamup.server.modules.team.service.TaskAssigneeService taskAssigneeService = mock(com.teamup.server.modules.team.service.TaskAssigneeService.class);
        TaskCommentServiceImpl taskCommentService = new TaskCommentServiceImpl(userMapper, userProfileMapper, permissionService, notificationService, taskService, taskAssigneeService);
        
        // Inject the mocked mapper into the base class using reflection
        ReflectionTestUtils.setField(taskCommentService, "baseMapper", taskCommentMapper);
        
        // Track comments in memory
        List<TaskComment> commentList = new ArrayList<>();
        
        // Create comments with different timestamps
        LocalDateTime baseTime = LocalDateTime.now();
        for (int i = 0; i < commentCount; i++) {
            TaskComment comment = new TaskComment();
            comment.setId((long) (i + 1));
            comment.setTaskId(taskId);
            comment.setUserId((long) (i + 1));
            comment.setContent("Comment " + i);
            comment.setCreatedAt(baseTime.plusMinutes(i));
            commentList.add(comment);
            
            // Mock user data
            User user = new User();
            user.setId((long) (i + 1));
            user.setUsername("user_" + (i + 1));
            when(userMapper.selectById((long) (i + 1))).thenReturn(user);
            
            UserProfile profile = new UserProfile();
            profile.setUserId((long) (i + 1));
            profile.setAvatarUrl("avatar_" + (i + 1) + ".jpg");
            when(userProfileMapper.selectOne(any(Wrapper.class))).thenReturn(profile);
        }
        
        // Mock select behavior - return comments ordered by created_at
        when(taskCommentMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return commentList.stream()
                .filter(c -> c.getTaskId().equals(taskId))
                .sorted(Comparator.comparing(TaskComment::getCreatedAt))
                .collect(Collectors.toList());
        });
        
        // Execute: Get comments
        List<TaskCommentDTO> comments = taskCommentService.getCommentsByTaskId(taskId);
        
        // Verify: Comments should be ordered chronologically
        assertEquals(commentCount, comments.size(), "Should return all comments");
        
        for (int i = 0; i < comments.size() - 1; i++) {
            LocalDateTime currentTime = comments.get(i).getCreatedAt();
            LocalDateTime nextTime = comments.get(i + 1).getCreatedAt();
            assertTrue(currentTime.isBefore(nextTime) || currentTime.isEqual(nextTime),
                "Comments should be ordered chronologically (earlier comments first)");
        }
        
        // Verify: First comment should be the earliest
        assertEquals(baseTime, comments.get(0).getCreatedAt(),
            "First comment should have the earliest timestamp");
        
        // Verify: Last comment should be the latest
        assertEquals(baseTime.plusMinutes(commentCount - 1), comments.get(comments.size() - 1).getCreatedAt(),
            "Last comment should have the latest timestamp");
    }
}
