package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.entity.TaskComment;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TaskAssigneeMapper;
import com.teamup.server.modules.team.mapper.TaskCommentMapper;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.impl.PermissionServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 权限控制服务属性测试
 * Feature: task-board-enhancement
 * 
 * 测试权限验证逻辑的正确性
 */
public class PermissionServicePropertyTest {

    /**
     * Property 9: 评论删除权限控制
     * For any comment, only the comment author should be able to delete it; 
     * deletion attempts by other users should be rejected with an authorization error.
     * Validates: Requirements 3.4
     */
    @Property(tries = 10)
    @Label("Feature: task-board-enhancement, Property 9: 评论删除权限控制")
    void onlyCommentAuthorCanDeleteComment(
            @ForAll @IntRange(min = 1, max = 10000) int commentIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int authorIdSeed,
            @ForAll @IntRange(min = 1001, max = 2000) int otherUserIdSeed) {
        
        Long commentId = (long) commentIdSeed;
        Long authorId = (long) authorIdSeed;
        Long otherUserId = (long) otherUserIdSeed;
        
        // Ensure author and other user are different
        Assume.that(!authorId.equals(otherUserId));
        
        // Setup mocks
        TaskCommentMapper taskCommentMapper = mock(TaskCommentMapper.class);
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskAssigneeMapper taskAssigneeMapper = mock(TaskAssigneeMapper.class);
        TeamMemberMapper teamMemberMapper = mock(TeamMemberMapper.class);
        
        PermissionServiceImpl permissionService = new PermissionServiceImpl(
            taskMapper,
            taskAssigneeMapper,
            taskCommentMapper,
            teamMemberMapper
        );
        
        // Mock comment data
        TaskComment comment = new TaskComment();
        comment.setId(commentId);
        comment.setUserId(authorId);
        comment.setTaskId(1L);
        comment.setContent("Test comment");
        comment.setCreatedAt(LocalDateTime.now());
        
        when(taskCommentMapper.selectById(commentId)).thenReturn(comment);
        
        // Test 1: Author can delete their own comment
        boolean authorCanDelete = permissionService.canDeleteComment(authorId, commentId);
        assertTrue(authorCanDelete, "Comment author should be able to delete their own comment");
        
        // Test 2: Other user cannot delete the comment
        boolean otherUserCanDelete = permissionService.canDeleteComment(otherUserId, commentId);
        assertFalse(otherUserCanDelete, "Other users should not be able to delete someone else's comment");
        
        // Note: We only test the permission logic, not the actual deletion operation
        // The actual deletion is tested in unit tests with proper Spring context
    }

    /**
     * Property 19: 任务编辑权限控制
     * For any task edit attempt, only the task creator or an assigned user should be able to edit the task; 
     * unauthorized edit attempts should be rejected with an authorization error.
     * Validates: Requirements 7.2
     */
    @Property(tries = 10)
    @Label("Feature: task-board-enhancement, Property 19: 任务编辑权限控制")
    void onlyCreatorOrAssigneeCanEditTask(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int creatorIdSeed,
            @ForAll @IntRange(min = 1001, max = 2000) int assigneeIdSeed,
            @ForAll @IntRange(min = 2001, max = 3000) int otherUserIdSeed) {
        
        Long taskId = (long) taskIdSeed;
        Long creatorId = (long) creatorIdSeed;
        Long assigneeId = (long) assigneeIdSeed;
        Long otherUserId = (long) otherUserIdSeed;
        
        // Ensure all users are different
        Assume.that(!creatorId.equals(assigneeId));
        Assume.that(!creatorId.equals(otherUserId));
        Assume.that(!assigneeId.equals(otherUserId));
        
        // Setup mocks
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskAssigneeMapper taskAssigneeMapper = mock(TaskAssigneeMapper.class);
        TaskCommentMapper taskCommentMapper = mock(TaskCommentMapper.class);
        TeamMemberMapper teamMemberMapper = mock(TeamMemberMapper.class);
        
        PermissionServiceImpl permissionService = new PermissionServiceImpl(
            taskMapper,
            taskAssigneeMapper,
            taskCommentMapper,
            teamMemberMapper
        );
        
        // Mock task data
        Task task = new Task();
        task.setId(taskId);
        task.setTeamId(1L);
        task.setTitle("Test Task");
        task.setCreatedBy(creatorId);
        task.setStatus("TODO");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        
        // Mock assignee check - assigneeId is an assignee
        when(taskAssigneeMapper.selectCount(any(Wrapper.class))).thenAnswer(invocation -> {
            // This is a simplified mock - in reality would check the wrapper conditions
            return 1L; // Assume assignee exists
        });
        
        // Test 1: Creator can edit task
        boolean creatorCanEdit = permissionService.canEditTask(creatorId, taskId);
        assertTrue(creatorCanEdit, "Task creator should be able to edit the task");
        
        // Test 2: Assignee can edit task
        boolean assigneeCanEdit = permissionService.canEditTask(assigneeId, taskId);
        assertTrue(assigneeCanEdit, "Task assignee should be able to edit the task");
        
        // Reset mock for assignee check
        when(taskAssigneeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        
        // Test 3: Other user cannot edit task
        boolean otherUserCanEdit = permissionService.canEditTask(otherUserId, taskId);
        assertFalse(otherUserCanEdit, "Other users should not be able to edit the task");
        
        // Note: We only test the permission logic, not the actual update operation
        // The actual update is tested in unit tests with proper Spring context
    }

    /**
     * Property 20: 任务删除权限控制
     * For any task deletion attempt, only the task creator or a team admin should be able to delete the task; 
     * unauthorized deletion attempts should be rejected with an authorization error.
     * Validates: Requirements 7.4
     */
    @Property(tries = 10)
    @Label("Feature: task-board-enhancement, Property 20: 任务删除权限控制")
    void onlyCreatorOrAdminCanDeleteTask(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 1, max = 100) int teamIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int creatorIdSeed,
            @ForAll @IntRange(min = 1001, max = 2000) int adminIdSeed,
            @ForAll @IntRange(min = 2001, max = 3000) int memberIdSeed) {
        
        Long taskId = (long) taskIdSeed;
        Long teamId = (long) teamIdSeed;
        Long creatorId = (long) creatorIdSeed;
        Long adminId = (long) adminIdSeed;
        Long memberId = (long) memberIdSeed;
        
        // Ensure all users are different
        Assume.that(!creatorId.equals(adminId));
        Assume.that(!creatorId.equals(memberId));
        Assume.that(!adminId.equals(memberId));
        
        // Setup mocks
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskAssigneeMapper taskAssigneeMapper = mock(TaskAssigneeMapper.class);
        TaskCommentMapper taskCommentMapper = mock(TaskCommentMapper.class);
        TeamMemberMapper teamMemberMapper = mock(TeamMemberMapper.class);
        
        PermissionServiceImpl permissionService = new PermissionServiceImpl(
            taskMapper,
            taskAssigneeMapper,
            taskCommentMapper,
            teamMemberMapper
        );
        
        // Mock task data
        Task task = new Task();
        task.setId(taskId);
        task.setTeamId(teamId);
        task.setTitle("Test Task");
        task.setCreatedBy(creatorId);
        task.setStatus("TODO");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        
        // Mock team member data
        TeamMember adminMember = new TeamMember();
        adminMember.setTeamId(teamId);
        adminMember.setUserId(adminId);
        adminMember.setRole("LEADER");
        
        TeamMember regularMember = new TeamMember();
        regularMember.setTeamId(teamId);
        regularMember.setUserId(memberId);
        regularMember.setRole("MEMBER");
        
        when(teamMemberMapper.selectByTeamAndUser(teamId, adminId)).thenReturn(adminMember);
        when(teamMemberMapper.selectByTeamAndUser(teamId, memberId)).thenReturn(regularMember);
        when(teamMemberMapper.selectByTeamAndUser(teamId, creatorId)).thenReturn(null);
        
        // Test 1: Creator can delete task
        boolean creatorCanDelete = permissionService.canDeleteTask(creatorId, taskId);
        assertTrue(creatorCanDelete, "Task creator should be able to delete the task");
        
        // Test 2: Team admin can delete task
        boolean adminCanDelete = permissionService.canDeleteTask(adminId, taskId);
        assertTrue(adminCanDelete, "Team admin should be able to delete any task");
        
        // Test 3: Regular member cannot delete task (if not creator)
        boolean memberCanDelete = permissionService.canDeleteTask(memberId, taskId);
        assertFalse(memberCanDelete, "Regular member should not be able to delete task created by others");
        
        // Note: We only test the permission logic, not the actual deletion operation
        // The actual deletion is tested in unit tests with proper Spring context
    }
}
