package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamup.server.modules.team.dto.TaskAssigneeDTO;
import com.teamup.server.modules.team.entity.TaskAssignee;

import java.util.List;

/**
 * 任务负责人服务接口
 * Requirements: 1.1, 1.2
 */
public interface TaskAssigneeService extends IService<TaskAssignee> {
    
    /**
     * 添加任务负责人
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务负责人DTO
     */
    TaskAssigneeDTO addAssignee(Long taskId, Long userId);
    
    /**
     * 移除任务负责人
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void removeAssignee(Long taskId, Long userId);
    
    /**
     * 获取任务的所有负责人
     * @param taskId 任务ID
     * @return 负责人DTO列表
     */
    List<TaskAssigneeDTO> getAssigneesByTaskId(Long taskId);
    
    /**
     * 获取用户负责的所有任务ID
     * @param userId 用户ID
     * @return 任务ID列表
     */
    List<Long> getTaskIdsByUserId(Long userId);
}
