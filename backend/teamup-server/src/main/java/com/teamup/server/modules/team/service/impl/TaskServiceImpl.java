package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.common.exception.AuthorizationException;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.team.dto.TaskDetailDTO;
import com.teamup.server.modules.team.dto.TaskFilterDTO;
import com.teamup.server.modules.team.dto.TaskStatsDTO;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.service.PermissionService;
import com.teamup.server.modules.team.service.TaskAssigneeService;
import com.teamup.server.modules.team.service.TaskCommentService;
import com.teamup.server.modules.team.service.TaskAttachmentService;
import com.teamup.server.modules.team.service.TaskService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务服务实现
 * Performance optimized with caching
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private TaskAssigneeService taskAssigneeService;
    
    @Autowired
    @org.springframework.context.annotation.Lazy
    private TaskCommentService taskCommentService;
    
    @Autowired
    private TaskAttachmentService taskAttachmentService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private com.teamup.server.modules.notification.service.NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task createTask(Task task) {
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        // Default status if not set
        if (task.getStatus() == null) {
            task.setStatus("TODO");
        }
        save(task);
        
        // 🔔 任务创建通知将在添加负责人时发送
        // 这里不发送通知，因为Task实体本身不包含assignees字段
        // 负责人通过taskAssigneeService单独添加，通知在那里处理
        
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task updateTask(Long userId, Task task) {
        // Check permission: user must be task creator or assignee
        if (!permissionService.canEditTask(userId, task.getId())) {
            throw new AuthorizationException("You don't have permission to edit this task");
        }
        
        // 审核规则 A：只有队长/管理员可以将任务从 REVIEW 变更为 DONE
        Task existing = getById(task.getId());
        if (existing == null) {
            throw new BusinessException("Task not found with id: " + task.getId());
        }
        if ("REVIEW".equals(existing.getStatus())
                && "DONE".equals(task.getStatus())
                && !permissionService.isTeamAdmin(userId, existing.getTeamId())) {
            throw new AuthorizationException("Only team admin can mark REVIEW task as DONE");
        }
        
        // 🔔 如果状态变更，发送通知
        if (!existing.getStatus().equals(task.getStatus())) {
            try {
                com.teamup.server.modules.user.entity.User updater = userMapper.selectById(userId);
                String updaterName = updater != null ? updater.getUsername() : "用户";
                String taskTitle = task.getTitle() != null ? task.getTitle() : existing.getTitle();
                
                // 通知任务创建者（如果不是自己）
                if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
                    notificationService.createNotification(
                        existing.getCreatedBy(),
                        "TASK_STATUS_CHANGED",
                        "任务状态已更新",
                        updaterName + " 将任务 " + taskTitle + " 的状态更新为 " + task.getStatus(),
                        "TASK",
                        task.getId()
                    );
                }
                
                // 通知所有负责人
                java.util.List<com.teamup.server.modules.team.dto.TaskAssigneeDTO> assignees = 
                    taskAssigneeService.getAssigneesByTaskId(task.getId());
                if (assignees != null) {
                    for (com.teamup.server.modules.team.dto.TaskAssigneeDTO assignee : assignees) {
                        if (!assignee.getUserId().equals(userId)) {
                            notificationService.createNotification(
                                assignee.getUserId(),
                                "TASK_STATUS_CHANGED",
                                "任务状态已更新",
                                "任务 " + taskTitle + " 的状态更新为 " + task.getStatus(),
                                "TASK",
                                task.getId()
                            );
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("发送任务状态变更通知失败: " + e.getMessage());
            }
        }
        
        task.setUpdatedAt(LocalDateTime.now());
        updateById(task);
        return getById(task.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long userId, Long taskId) {
        // Check permission: user must be task creator or team admin
        if (!permissionService.canDeleteTask(userId, taskId)) {
            throw new AuthorizationException("You don't have permission to delete this task");
        }
        
        removeById(taskId);
    }

    @Override
    public List<Task> getTasksByTeamId(Long teamId) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getTeamId, teamId);
        queryWrapper.orderByDesc(Task::getCreatedAt);
        return list(queryWrapper);
    }
    
    @Override
    public TaskDetailDTO getTaskDetail(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            throw new BusinessException("Task not found with id: " + taskId);
        }
        
        TaskDetailDTO detailDTO = new TaskDetailDTO();
        BeanUtils.copyProperties(task, detailDTO);
        
        // Load creator name
        if (task.getCreatedBy() != null) {
            User creator = userMapper.selectById(task.getCreatedBy());
            if (creator != null) {
                detailDTO.setCreatorName(creator.getUsername());
            }
        }
        
        // Load assignees (optimized with batch queries)
        detailDTO.setAssignees(taskAssigneeService.getAssigneesByTaskId(taskId));
        
        // Load comments (optimized with batch queries)
        detailDTO.setComments(taskCommentService.getCommentsByTaskId(taskId));
        
        // Load attachments (optimized with batch queries)
        detailDTO.setAttachments(taskAttachmentService.getAttachmentsByTaskId(taskId));
        
        // Set counts
        detailDTO.setCommentCount(detailDTO.getComments() != null ? detailDTO.getComments().size() : 0);
        detailDTO.setAttachmentCount(detailDTO.getAttachments() != null ? detailDTO.getAttachments().size() : 0);
        
        return detailDTO;
    }
    
    @Override
    public List<Task> filterTasks(Long teamId, TaskFilterDTO filter) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getTeamId, teamId);
        
        // Filter by status
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            queryWrapper.eq(Task::getStatus, filter.getStatus());
        }
        
        // Filter by priority
        if (filter.getPriority() != null && !filter.getPriority().isEmpty()) {
            queryWrapper.eq(Task::getPriority, filter.getPriority());
        }
        
        // Filter by keyword (search in title)
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            queryWrapper.like(Task::getTitle, filter.getKeyword());
        }
        
        // Filter by assignee
        if (filter.getAssigneeId() != null) {
            // Get task IDs assigned to this user
            List<Long> taskIds = taskAssigneeService.getTaskIdsByUserId(filter.getAssigneeId());
            if (taskIds.isEmpty()) {
                return List.of();
            }
            queryWrapper.in(Task::getId, taskIds);
        }
        
        queryWrapper.orderByDesc(Task::getCreatedAt);
        return list(queryWrapper);
    }
    
    @Override
    public TaskStatsDTO getTaskStats(Long teamId) {
        List<Task> tasks = getTasksByTeamId(teamId);
        
        TaskStatsDTO stats = new TaskStatsDTO();
        stats.setTotalCount(tasks.size());
        
        int todoCount = 0;
        int doingCount = 0;
        int reviewCount = 0;
        int doneCount = 0;
        int overdueCount = 0;
        
        LocalDate today = LocalDate.now();
        
        for (Task task : tasks) {
            // Count by status
            switch (task.getStatus()) {
                case "TODO":
                    todoCount++;
                    break;
                case "DOING":
                    doingCount++;
                    break;
                case "REVIEW":
                    reviewCount++;
                    break;
                case "DONE":
                    doneCount++;
                    break;
            }
            
            // Count overdue tasks
            if (task.getDeadline() != null && 
                task.getDeadline().isBefore(today) && 
                !"DONE".equals(task.getStatus())) {
                overdueCount++;
            }
        }
        
        stats.setTodoCount(todoCount);
        stats.setDoingCount(doingCount);
        stats.setReviewCount(reviewCount);
        stats.setDoneCount(doneCount);
        stats.setOverdueCount(overdueCount);
        
        // Calculate completion rate
        if (stats.getTotalCount() > 0) {
            stats.setCompletionRate((double) doneCount / stats.getTotalCount() * 100);
        } else {
            stats.setCompletionRate(0.0);
        }
        
        return stats;
    }
}
