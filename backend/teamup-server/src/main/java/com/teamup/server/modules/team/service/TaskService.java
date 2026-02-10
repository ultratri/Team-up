package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamup.server.modules.team.dto.TaskDetailDTO;
import com.teamup.server.modules.team.dto.TaskFilterDTO;
import com.teamup.server.modules.team.dto.TaskStatsDTO;
import com.teamup.server.modules.team.entity.Task;

import java.util.List;

public interface TaskService extends IService<Task> {
    Task createTask(Task task);
    Task updateTask(Long userId, Task task);
    void deleteTask(Long userId, Long taskId);
    List<Task> getTasksByTeamId(Long teamId);
    TaskDetailDTO getTaskDetail(Long taskId);
    List<Task> filterTasks(Long teamId, TaskFilterDTO filter);
    TaskStatsDTO getTaskStats(Long teamId);
}
