package com.teamup.server.modules.team.controller;

import com.teamup.server.modules.team.dto.TaskDetailDTO;
import com.teamup.server.modules.team.dto.TaskFilterDTO;
import com.teamup.server.modules.team.dto.TaskStatsDTO;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.service.TaskService;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.user.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final JwtUtil jwtUtil;

    private Long resolveUserId(HttpServletRequest request, Long userIdParam) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        // 兼容旧前端：如果没有 JWT，再使用 userId 参数
        if (userIdParam != null) return userIdParam;
        return null;
    }

    @GetMapping("/team/{teamId}")
    public Result<List<Task>> getTeamTasks(@PathVariable Long teamId) {
        return Result.success(taskService.getTasksByTeamId(teamId));
    }
    
    @GetMapping("/{id}")
    public Result<TaskDetailDTO> getTaskDetail(@PathVariable Long id) {
        return Result.success(taskService.getTaskDetail(id));
    }
    
    @GetMapping("/team/{teamId}/filter")
    public Result<List<Task>> filterTasks(@PathVariable Long teamId, TaskFilterDTO filter) {
        return Result.success(taskService.filterTasks(teamId, filter));
    }
    
    @GetMapping("/team/{teamId}/stats")
    public Result<TaskStatsDTO> getTaskStats(@PathVariable Long teamId) {
        return Result.success(taskService.getTaskStats(teamId));
    }

    @PostMapping
    public Result<Task> createTask(@RequestBody Task task) {
        return Result.success(taskService.createTask(task));
    }

    @PutMapping
    public Result<Task> updateTask(
            HttpServletRequest request,
            @RequestBody Task task,
            @RequestParam(required = false) Long userId) {
        Long resolvedUserId = resolveUserId(request, userId);
        if (resolvedUserId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return Result.success(taskService.updateTask(resolvedUserId, task));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        Long resolvedUserId = resolveUserId(request, userId);
        if (resolvedUserId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        taskService.deleteTask(resolvedUserId, id);
        return Result.success();
    }
}
