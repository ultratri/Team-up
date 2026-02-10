package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.entity.TaskComment;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TaskAssigneeMapper;
import com.teamup.server.modules.team.mapper.TaskCommentMapper;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 权限验证服务实现
 * 
 * <p>提供任务相关的权限验证功能，包括：
 * <ul>
 *   <li>任务编辑权限：任务创建者或负责人可以编辑</li>
 *   <li>任务删除权限：任务创建者或团队管理员可以删除</li>
 *   <li>评论删除权限：只有评论作者可以删除自己的评论</li>
 *   <li>团队成员验证：检查用户是否为团队成员</li>
 *   <li>团队管理员验证：检查用户是否为团队管理员</li>
 * </ul>
 * 
 * @author TeamUp
 * @version 1.0
 * @see PermissionService
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    
    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final TeamMemberMapper teamMemberMapper;
    
    /**
     * 检查用户是否可以编辑任务
     * 
     * <p>编辑权限规则：
     * <ul>
     *   <li>任务创建者可以编辑</li>
     *   <li>任务负责人可以编辑</li>
     * </ul>
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return true 如果用户有编辑权限，否则返回 false
     */
    @Override
    public boolean canEditTask(Long userId, Long taskId) {
        // Validate input parameters
        if (userId == null || taskId == null) {
            log.warn("canEditTask called with null parameters: userId={}, taskId={}", userId, taskId);
            return false;
        }
        
        // Get the task
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("Task not found: taskId={}", taskId);
            return false;
        }

        // 规则调整：团队成员允许推进任务流程（看板拖拽），最终完成由审核规则控制
        if (isTeamMember(userId, task.getTeamId())) {
            return true;
        }
        
        // Check if user is the task creator
        if (task.getCreatedBy().equals(userId)) {
            log.debug("User {} is task creator, granting edit permission", userId);
            return true;
        }
        
        // Check if user is an assignee
        boolean isAssignee = isTaskAssignee(userId, taskId);
        if (isAssignee) {
            log.debug("User {} is task assignee, granting edit permission", userId);
        }
        
        return isAssignee;
    }
    
    /**
     * 检查用户是否可以删除任务
     * 
     * <p>删除权限规则：
     * <ul>
     *   <li>团队管理员可以删除任何任务</li>
     *   <li>任务创建者可以删除自己创建的任务</li>
     * </ul>
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return true 如果用户有删除权限，否则返回 false
     */
    @Override
    public boolean canDeleteTask(Long userId, Long taskId) {
        // Validate input parameters
        if (userId == null || taskId == null) {
            log.warn("canDeleteTask called with null parameters: userId={}, taskId={}", userId, taskId);
            return false;
        }
        
        // Get the task
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("Task not found: taskId={}", taskId);
            return false;
        }
        
        // Check if user is team admin
        if (isTeamAdmin(userId, task.getTeamId())) {
            log.debug("User {} is team admin, granting delete permission", userId);
            return true;
        }
        
        // Check if user is the task creator
        boolean isCreator = task.getCreatedBy().equals(userId);
        if (isCreator) {
            log.debug("User {} is task creator, granting delete permission", userId);
        }
        
        return isCreator;
    }
    
    /**
     * 检查用户是否可以删除评论
     * 
     * <p>删除权限规则：
     * <ul>
     *   <li>只有评论作者可以删除自己的评论</li>
     * </ul>
     * 
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return true 如果用户有删除权限，否则返回 false
     */
    @Override
    public boolean canDeleteComment(Long userId, Long commentId) {
        // Validate input parameters
        if (userId == null || commentId == null) {
            log.warn("canDeleteComment called with null parameters: userId={}, commentId={}", userId, commentId);
            return false;
        }
        
        // Get the comment
        TaskComment comment = taskCommentMapper.selectById(commentId);
        if (comment == null) {
            log.warn("Comment not found: commentId={}", commentId);
            return false;
        }
        
        // Only the comment author can delete it
        boolean isAuthor = comment.getUserId().equals(userId);
        if (isAuthor) {
            log.debug("User {} is comment author, granting delete permission", userId);
        }
        
        return isAuthor;
    }
    
    /**
     * 检查用户是否为团队成员
     * 
     * @param userId 用户ID
     * @param teamId 团队ID
     * @return true 如果用户是团队成员，否则返回 false
     */
    @Override
    public boolean isTeamMember(Long userId, Long teamId) {
        // Validate input parameters
        if (userId == null || teamId == null) {
            log.warn("isTeamMember called with null parameters: userId={}, teamId={}", userId, teamId);
            return false;
        }
        
        TeamMember member = teamMemberMapper.selectByTeamAndUser(teamId, userId);
        return member != null;
    }
    
    /**
     * 检查用户是否为团队管理员
     * 
     * @param userId 用户ID
     * @param teamId 团队ID
     * @return true 如果用户是团队管理员（角色为LEADER），否则返回 false
     */
    @Override
    public boolean isTeamAdmin(Long userId, Long teamId) {
        // Validate input parameters
        if (userId == null || teamId == null) {
            log.warn("isTeamAdmin called with null parameters: userId={}, teamId={}", userId, teamId);
            return false;
        }
        
        TeamMember member = teamMemberMapper.selectByTeamAndUser(teamId, userId);
        if (member == null) {
            return false;
        }
        
        // Check if the role is LEADER (admin)
        return "LEADER".equals(member.getRole());
    }
    
    /**
     * 检查用户是否为任务负责人（私有辅助方法）
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return true 如果用户是任务负责人，否则返回 false
     */
    private boolean isTaskAssignee(Long userId, Long taskId) {
        LambdaQueryWrapper<TaskAssignee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskAssignee::getTaskId, taskId)
                   .eq(TaskAssignee::getUserId, userId);
        Long count = taskAssigneeMapper.selectCount(queryWrapper);
        
        return count > 0;
    }
}
