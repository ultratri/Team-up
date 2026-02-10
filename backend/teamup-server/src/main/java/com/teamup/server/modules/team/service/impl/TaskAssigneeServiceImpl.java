package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.team.dto.TaskAssigneeDTO;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.mapper.TaskAssigneeMapper;
import com.teamup.server.modules.team.service.TaskAssigneeService;
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
 * 任务负责人服务实现
 * Requirements: 1.1, 1.2
 */
@Service
@RequiredArgsConstructor
public class TaskAssigneeServiceImpl extends ServiceImpl<TaskAssigneeMapper, TaskAssignee> implements TaskAssigneeService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final com.teamup.server.modules.notification.service.NotificationService notificationService;
    private final com.teamup.server.modules.team.mapper.TaskMapper taskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskAssigneeDTO addAssignee(Long taskId, Long userId) {
        // Check if assignee already exists
        LambdaQueryWrapper<TaskAssignee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskAssignee::getTaskId, taskId)
                   .eq(TaskAssignee::getUserId, userId);
        
        TaskAssignee existing = getOne(queryWrapper);
        if (existing != null) {
            // Already assigned, return existing
            return convertToDTO(existing);
        }
        
        // Create new assignee
        TaskAssignee assignee = new TaskAssignee();
        assignee.setTaskId(taskId);
        assignee.setUserId(userId);
        assignee.setAssignedAt(LocalDateTime.now());
        
        save(assignee);
        
        // 🔔 发送任务分配通知
        try {
            com.teamup.server.modules.team.entity.Task task = taskMapper.selectById(taskId);
            if (task != null && task.getCreatedBy() != null && !userId.equals(task.getCreatedBy())) {
                User creator = userMapper.selectById(task.getCreatedBy());
                String creatorName = creator != null ? creator.getUsername() : "用户";
                String taskTitle = task.getTitle() != null ? task.getTitle() : "新任务";
                
                notificationService.createNotification(
                    userId,
                    "TASK_ASSIGNED",
                    creatorName + " 给你分配了任务",
                    "任务：" + taskTitle + 
                    (task.getDeadline() != null ? "\n截止时间：" + task.getDeadline() : ""),
                    "TASK",
                    taskId
                );
            }
        } catch (Exception e) {
            // 通知失败不影响任务分配
            System.err.println("发送任务分配通知失败: " + e.getMessage());
        }
        
        return convertToDTO(assignee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAssignee(Long taskId, Long userId) {
        LambdaQueryWrapper<TaskAssignee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskAssignee::getTaskId, taskId)
                   .eq(TaskAssignee::getUserId, userId);
        
        remove(queryWrapper);
    }

    @Override
    public List<TaskAssigneeDTO> getAssigneesByTaskId(Long taskId) {
        LambdaQueryWrapper<TaskAssignee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskAssignee::getTaskId, taskId)
                   .orderByAsc(TaskAssignee::getAssignedAt);
        
        List<TaskAssignee> assignees = list(queryWrapper);
        
        // Batch query optimization: collect all user IDs first
        List<Long> userIds = assignees.stream()
                .map(TaskAssignee::getUserId)
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
        List<TaskAssigneeDTO> dtoList = new ArrayList<>();
        for (TaskAssignee assignee : assignees) {
            dtoList.add(convertToDTOWithCache(assignee, userMap, profileMap));
        }
        
        return dtoList;
    }
    
    @Override
    public List<Long> getTaskIdsByUserId(Long userId) {
        LambdaQueryWrapper<TaskAssignee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskAssignee::getUserId, userId);
        
        List<TaskAssignee> assignees = list(queryWrapper);
        List<Long> taskIds = new ArrayList<>();
        
        for (TaskAssignee assignee : assignees) {
            taskIds.add(assignee.getTaskId());
        }
        
        return taskIds;
    }
    
    /**
     * 转换实体为DTO
     */
    private TaskAssigneeDTO convertToDTO(TaskAssignee assignee) {
        TaskAssigneeDTO dto = new TaskAssigneeDTO();
        dto.setId(assignee.getId());
        dto.setTaskId(assignee.getTaskId());
        dto.setUserId(assignee.getUserId());
        dto.setAssignedAt(assignee.getAssignedAt());
        
        // Get user information
        User user = userMapper.selectById(assignee.getUserId());
        if (user != null) {
            dto.setUserName(user.getUsername());
            
            // Get user avatar from profile
            LambdaQueryWrapper<UserProfile> profileQuery = new LambdaQueryWrapper<>();
            profileQuery.eq(UserProfile::getUserId, assignee.getUserId());
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
    private TaskAssigneeDTO convertToDTOWithCache(
            TaskAssignee assignee,
            java.util.Map<Long, User> userMap,
            java.util.Map<Long, UserProfile> profileMap) {
        TaskAssigneeDTO dto = new TaskAssigneeDTO();
        dto.setId(assignee.getId());
        dto.setTaskId(assignee.getTaskId());
        dto.setUserId(assignee.getUserId());
        dto.setAssignedAt(assignee.getAssignedAt());
        
        // Get user information from cache
        User user = userMap.get(assignee.getUserId());
        if (user != null) {
            dto.setUserName(user.getUsername());
            
            // Get user avatar from cache
            UserProfile profile = profileMap.get(assignee.getUserId());
            if (profile != null) {
                dto.setAvatar(profile.getAvatarUrl());
            }
        }
        
        return dto;
    }
}
