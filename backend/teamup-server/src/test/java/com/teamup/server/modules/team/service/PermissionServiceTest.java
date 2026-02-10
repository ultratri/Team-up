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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 权限服务单元测试
 * Requirements: 7.2, 7.4, 7.5
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskAssigneeMapper taskAssigneeMapper;

    @Mock
    private TaskCommentMapper taskCommentMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionServiceImpl(
            taskMapper,
            taskAssigneeMapper,
            taskCommentMapper,
            teamMemberMapper
        );
    }

    /**
     * 测试授权用户可以编辑任务 - 任务创建者
     * Requirements: 7.2
     */
    @Test
    void testCanEditTask_Creator_Success() {
        // Given
        Long userId = 1L;
        Long taskId = 100L;
        
        Task task = new Task();
        task.setId(taskId);
        task.setCreatedBy(userId);
        task.setTeamId(1L);
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        
        // When
        boolean canEdit = permissionService.canEditTask(userId, taskId);
        
        // Then
        assertTrue(canEdit, "Task creator should be able to edit the task");
    }

    /**
     * 测试授权用户可以编辑任务 - 任务负责人
     * Requirements: 7.2
     */
    @Test
    void testCanEditTask_Assignee_Success() {
        // Given
        Long userId = 2L;
        Long taskId = 100L;
        Long creatorId = 1L;
        
        Task task = new Task();
        task.setId(taskId);
        task.setCreatedBy(creatorId);
        task.setTeamId(1L);
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskAssigneeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        
        // When
        boolean canEdit = permissionService.canEditTask(userId, taskId);
        
        // Then
        assertTrue(canEdit, "Task assignee should be able to edit the task");
    }

    /**
     * 测试未授权用户不能编辑任务
     * Requirements: 7.2
     */
    @Test
    void testCanEditTask_UnauthorizedUser_Failure() {
        // Given
        Long userId = 3L;
        Long taskId = 100L;
        Long creatorId = 1L;
        
        Task task = new Task();
        task.setId(taskId);
        task.setCreatedBy(creatorId);
        task.setTeamId(1L);
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskAssigneeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        
        // When
        boolean canEdit = permissionService.canEditTask(userId, taskId);
        
        // Then
        assertFalse(canEdit, "Unauthorized user should not be able to edit the task");
    }

    /**
     * 测试管理员可以删除任何任务
     * Requirements: 7.4, 7.5
     */
    @Test
    void testCanDeleteTask_Admin_Success() {
        // Given
        Long adminId = 2L;
        Long taskId = 100L;
        Long creatorId = 1L;
        Long teamId = 1L;
        
        Task task = new Task();
        task.setId(taskId);
        task.setCreatedBy(creatorId);
        task.setTeamId(teamId);
        
        TeamMember adminMember = new TeamMember();
        adminMember.setUserId(adminId);
        adminMember.setTeamId(teamId);
        adminMember.setRole("LEADER");
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(teamMemberMapper.selectByTeamAndUser(teamId, adminId)).thenReturn(adminMember);
        
        // When
        boolean canDelete = permissionService.canDeleteTask(adminId, taskId);
        
        // Then
        assertTrue(canDelete, "Team admin should be able to delete any task");
    }

    /**
     * 测试普通成员只能删除自己的任务
     * Requirements: 7.4, 7.5
     */
    @Test
    void testCanDeleteTask_MemberOwnTask_Success() {
        // Given
        Long userId = 1L;
        Long taskId = 100L;
        Long teamId = 1L;
        
        Task task = new Task();
        task.setId(taskId);
        task.setCreatedBy(userId);
        task.setTeamId(teamId);
        
        TeamMember member = new TeamMember();
        member.setUserId(userId);
        member.setTeamId(teamId);
        member.setRole("MEMBER");
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(teamMemberMapper.selectByTeamAndUser(teamId, userId)).thenReturn(member);
        
        // When
        boolean canDelete = permissionService.canDeleteTask(userId, taskId);
        
        // Then
        assertTrue(canDelete, "Member should be able to delete their own task");
    }

    /**
     * 测试普通成员不能删除他人的任务
     * Requirements: 7.4, 7.5
     */
    @Test
    void testCanDeleteTask_MemberOthersTask_Failure() {
        // Given
        Long userId = 2L;
        Long taskId = 100L;
        Long creatorId = 1L;
        Long teamId = 1L;
        
        Task task = new Task();
        task.setId(taskId);
        task.setCreatedBy(creatorId);
        task.setTeamId(teamId);
        
        TeamMember member = new TeamMember();
        member.setUserId(userId);
        member.setTeamId(teamId);
        member.setRole("MEMBER");
        
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(teamMemberMapper.selectByTeamAndUser(teamId, userId)).thenReturn(member);
        
        // When
        boolean canDelete = permissionService.canDeleteTask(userId, taskId);
        
        // Then
        assertFalse(canDelete, "Member should not be able to delete others' tasks");
    }

    /**
     * 测试只能删除自己的评论
     * Requirements: 3.4
     */
    @Test
    void testCanDeleteComment_Author_Success() {
        // Given
        Long userId = 1L;
        Long commentId = 100L;
        
        TaskComment comment = new TaskComment();
        comment.setId(commentId);
        comment.setUserId(userId);
        comment.setTaskId(1L);
        comment.setContent("Test comment");
        comment.setCreatedAt(LocalDateTime.now());
        
        when(taskCommentMapper.selectById(commentId)).thenReturn(comment);
        
        // When
        boolean canDelete = permissionService.canDeleteComment(userId, commentId);
        
        // Then
        assertTrue(canDelete, "Comment author should be able to delete their own comment");
    }

    /**
     * 测试不能删除他人的评论
     * Requirements: 3.4
     */
    @Test
    void testCanDeleteComment_NotAuthor_Failure() {
        // Given
        Long userId = 2L;
        Long commentId = 100L;
        Long authorId = 1L;
        
        TaskComment comment = new TaskComment();
        comment.setId(commentId);
        comment.setUserId(authorId);
        comment.setTaskId(1L);
        comment.setContent("Test comment");
        comment.setCreatedAt(LocalDateTime.now());
        
        when(taskCommentMapper.selectById(commentId)).thenReturn(comment);
        
        // When
        boolean canDelete = permissionService.canDeleteComment(userId, commentId);
        
        // Then
        assertFalse(canDelete, "Non-author should not be able to delete others' comments");
    }

    /**
     * 测试团队成员检查
     * Requirements: 7.1
     */
    @Test
    void testIsTeamMember_Success() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        
        TeamMember member = new TeamMember();
        member.setUserId(userId);
        member.setTeamId(teamId);
        member.setRole("MEMBER");
        
        when(teamMemberMapper.selectByTeamAndUser(teamId, userId)).thenReturn(member);
        
        // When
        boolean isMember = permissionService.isTeamMember(userId, teamId);
        
        // Then
        assertTrue(isMember, "User should be identified as team member");
    }

    /**
     * 测试团队管理员检查
     * Requirements: 7.1
     */
    @Test
    void testIsTeamAdmin_Success() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        
        TeamMember member = new TeamMember();
        member.setUserId(userId);
        member.setTeamId(teamId);
        member.setRole("LEADER");
        
        when(teamMemberMapper.selectByTeamAndUser(teamId, userId)).thenReturn(member);
        
        // When
        boolean isAdmin = permissionService.isTeamAdmin(userId, teamId);
        
        // Then
        assertTrue(isAdmin, "User with LEADER role should be identified as team admin");
    }

    /**
     * 测试非管理员检查
     * Requirements: 7.1
     */
    @Test
    void testIsTeamAdmin_RegularMember_Failure() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        
        TeamMember member = new TeamMember();
        member.setUserId(userId);
        member.setTeamId(teamId);
        member.setRole("MEMBER");
        
        when(teamMemberMapper.selectByTeamAndUser(teamId, userId)).thenReturn(member);
        
        // When
        boolean isAdmin = permissionService.isTeamAdmin(userId, teamId);
        
        // Then
        assertFalse(isAdmin, "User with MEMBER role should not be identified as team admin");
    }
}
