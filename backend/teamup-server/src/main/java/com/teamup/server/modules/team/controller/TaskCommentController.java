package com.teamup.server.modules.team.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.dto.TaskCommentDTO;
import com.teamup.server.modules.team.service.TaskCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务评论控制器
 * Requirements: 3.1, 3.2, 3.3, 3.5
 */
@RestController
@RequestMapping("/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    /**
     * 添加任务评论
     * POST /tasks/{taskId}/comments
     * 
     * @param taskId 任务ID
     * @param request 请求体，包含userId和content
     * @return 评论DTO
     */
    @PostMapping
    public Result<TaskCommentDTO> addComment(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> request) {
        
        Object userIdObj = request.get("userId");
        String content = (String) request.get("content");
        
        // Validate required fields
        if (userIdObj == null) {
            return Result.error(400, "userId is required");
        }
        
        if (content == null || content.trim().isEmpty()) {
            return Result.error(400, "content is required");
        }
        
        try {
            Long userId = null;
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else {
                return Result.error(400, "userId must be a number");
            }
            
            TaskCommentDTO comment = taskCommentService.addComment(taskId, userId, content);
            return Result.success(comment);
        } catch (Exception e) {
            return Result.error("Failed to add comment: " + e.getMessage());
        }
    }

    /**
     * 删除任务评论
     * DELETE /tasks/{taskId}/comments/{commentId}?userId={userId}
     * 
     * @param taskId 任务ID
     * @param commentId 评论ID
     * @param userId 用户ID (用于权限验证)
     * @return 成功响应
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        
        try {
            taskCommentService.deleteComment(userId, commentId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("Failed to delete comment: " + e.getMessage());
        }
    }

    /**
     * 获取任务的所有评论
     * GET /tasks/{taskId}/comments
     * 
     * @param taskId 任务ID
     * @return 评论DTO列表（按时间排序）
     */
    @GetMapping
    public Result<List<TaskCommentDTO>> getComments(@PathVariable Long taskId) {
        try {
            List<TaskCommentDTO> comments = taskCommentService.getCommentsByTaskId(taskId);
            return Result.success(comments);
        } catch (Exception e) {
            return Result.error("Failed to get comments: " + e.getMessage());
        }
    }
}
