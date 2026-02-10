package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.common.exception.AuthorizationException;
import com.teamup.server.modules.team.dto.TaskCommentDTO;
import com.teamup.server.modules.team.entity.TaskComment;
import com.teamup.server.modules.team.mapper.TaskCommentMapper;
import com.teamup.server.modules.team.service.PermissionService;
import com.teamup.server.modules.team.service.TaskCommentService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务评论服务实现
 * Requirements: 3.1, 3.2, 3.3
 */
@Service
@RequiredArgsConstructor
public class TaskCommentServiceImpl extends ServiceImpl<TaskCommentMapper, TaskComment> implements TaskCommentService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final PermissionService permissionService;
    private final com.teamup.server.modules.notification.service.NotificationService notificationService;
    private final com.teamup.server.modules.team.service.TaskService taskService;
    private final com.teamup.server.modules.team.service.TaskAssigneeService taskAssigneeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskCommentDTO addComment(Long taskId, Long userId, String content) {
        // Create new comment
        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        
        save(comment);
        
        // 🔔 通知任务创建者和负责人
        try {
            com.teamup.server.modules.team.entity.Task task = taskService.getById(taskId);
            if (task != null) {
                User commenter = userMapper.selectById(userId);
                String commenterName = commenter != null ? commenter.getUsername() : "用户";
                String taskTitle = task.getTitle() != null ? task.getTitle() : "任务";
                
                // 通知任务创建者（如果不是自己）
                if (task.getCreatedBy() != null && !task.getCreatedBy().equals(userId)) {
                    notificationService.createNotification(
                        task.getCreatedBy(),
                        "TASK_COMMENTED",
                        commenterName + " 评论了任务",
                        "任务：" + taskTitle + "\n评论：" + content,
                        "TASK",
                        taskId
                    );
                }
                
                // 通知所有负责人（如果不是自己）
                java.util.List<com.teamup.server.modules.team.dto.TaskAssigneeDTO> assignees = 
                    taskAssigneeService.getAssigneesByTaskId(taskId);
                if (assignees != null) {
                    for (com.teamup.server.modules.team.dto.TaskAssigneeDTO assignee : assignees) {
                        if (!assignee.getUserId().equals(userId)) {
                            notificationService.createNotification(
                                assignee.getUserId(),
                                "TASK_COMMENTED",
                                commenterName + " 评论了任务",
                                "任务：" + taskTitle + "\n评论：" + content,
                                "TASK",
                                taskId
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("发送任务评论通知失败: " + e.getMessage());
        }
        
        return convertToDTO(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        // Check permission: only comment author can delete
        if (!permissionService.canDeleteComment(userId, commentId)) {
            throw new AuthorizationException("You don't have permission to delete this comment");
        }
        
        removeById(commentId);
    }

    @Override
    public List<TaskCommentDTO> getCommentsByTaskId(Long taskId) {
        // Query comments ordered by creation time (chronological order)
        LambdaQueryWrapper<TaskComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskComment::getTaskId, taskId)
                   .orderByAsc(TaskComment::getCreatedAt);
        
        List<TaskComment> comments = list(queryWrapper);
        
        // Batch query optimization: collect all user IDs first
        List<Long> userIds = comments.stream()
                .map(TaskComment::getUserId)
                .distinct()
                .toList();
        
        if (userIds.isEmpty()) {
            return List.of();
        }
        
        // Batch query users
        List<User> users = userMapper.selectBatchIds(userIds);
        
        // Batch query profiles
        LambdaQueryWrapper<UserProfile> profileQuery = new LambdaQueryWrapper<>();
        profileQuery.in(UserProfile::getUserId, userIds);
        List<UserProfile> profiles = userProfileMapper.selectList(profileQuery);
        
        // Create maps for O(1) lookup
        var userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        var profileMap = profiles.stream()
                .collect(java.util.stream.Collectors.toMap(UserProfile::getUserId, p -> p));
        
        // Convert to DTOs using cached data
        List<TaskCommentDTO> dtoList = new ArrayList<>();
        for (TaskComment comment : comments) {
            dtoList.add(convertToDTOWithCache(comment, userMap, profileMap));
        }
        
        return dtoList;
    }
    
    /**
     * 转换实体为DTO
     */
    private TaskCommentDTO convertToDTO(TaskComment comment) {
        TaskCommentDTO dto = new TaskCommentDTO();
        dto.setId(comment.getId());
        dto.setTaskId(comment.getTaskId());
        dto.setUserId(comment.getUserId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        
        // Get user information
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            dto.setUserName(user.getUsername());
            
            // Get user avatar from profile
            LambdaQueryWrapper<UserProfile> profileQuery = new LambdaQueryWrapper<>();
            profileQuery.eq(UserProfile::getUserId, comment.getUserId());
            UserProfile profile = userProfileMapper.selectOne(profileQuery);
            if (profile != null) {
                dto.setAvatar(profile.getAvatarUrl());
            }
        }
        
        return dto;
    }
    
    /**
     * 转换实体为DTO（使用缓存数据，避免N+1查询）
     */
    private TaskCommentDTO convertToDTOWithCache(
            TaskComment comment,
            java.util.Map<Long, User> userMap,
            java.util.Map<Long, UserProfile> profileMap) {
        TaskCommentDTO dto = new TaskCommentDTO();
        dto.setId(comment.getId());
        dto.setTaskId(comment.getTaskId());
        dto.setUserId(comment.getUserId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        
        // Get user information from cache
        User user = userMap.get(comment.getUserId());
        if (user != null) {
            dto.setUserName(user.getUsername());
            
            // Get user avatar from cache
            UserProfile profile = profileMap.get(comment.getUserId());
            if (profile != null) {
                dto.setAvatar(profile.getAvatarUrl());
            }
        }
        
        return dto;
    }
}
