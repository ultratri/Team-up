package com.teamup.server.modules.team.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.dto.TaskAssigneeDTO;
import com.teamup.server.modules.team.service.TaskAssigneeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务负责人控制器
 * Requirements: 1.1, 1.2, 1.3
 */
@RestController
@RequestMapping("/tasks/{taskId}/assignees")
@RequiredArgsConstructor
public class TaskAssigneeController {

    private final TaskAssigneeService taskAssigneeService;

    /**
     * 添加任务负责人
     * POST /tasks/{taskId}/assignees
     * 
     * @param taskId 任务ID
     * @param request 请求体，包含userId
     * @return 任务负责人DTO
     */
    @PostMapping
    public Result<TaskAssigneeDTO> addAssignee(
            @PathVariable Long taskId,
            @RequestBody Map<String, Long> request) {
        
        Long userId = request.get("userId");
        if (userId == null) {
            return Result.error(400, "userId is required");
        }
        
        try {
            TaskAssigneeDTO assignee = taskAssigneeService.addAssignee(taskId, userId);
            return Result.success(assignee);
        } catch (Exception e) {
            return Result.error("Failed to add assignee: " + e.getMessage());
        }
    }

    /**
     * 移除任务负责人
     * DELETE /tasks/{taskId}/assignees/{userId}
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 成功响应
     */
    @DeleteMapping("/{userId}")
    public Result<Void> removeAssignee(
            @PathVariable Long taskId,
            @PathVariable Long userId) {
        
        try {
            taskAssigneeService.removeAssignee(taskId, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("Failed to remove assignee: " + e.getMessage());
        }
    }

    /**
     * 获取任务的所有负责人
     * GET /tasks/{taskId}/assignees
     * 
     * @param taskId 任务ID
     * @return 负责人DTO列表
     */
    @GetMapping
    public Result<List<TaskAssigneeDTO>> getAssignees(@PathVariable Long taskId) {
        try {
            List<TaskAssigneeDTO> assignees = taskAssigneeService.getAssigneesByTaskId(taskId);
            return Result.success(assignees);
        } catch (Exception e) {
            return Result.error("Failed to get assignees: " + e.getMessage());
        }
    }
}
