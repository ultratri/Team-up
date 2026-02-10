package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamup.server.modules.team.dto.TaskCommentDTO;
import com.teamup.server.modules.team.entity.TaskComment;

import java.util.List;

/**
 * 任务评论服务接口
 * Requirements: 3.1, 3.2, 3.3
 */
public interface TaskCommentService extends IService<TaskComment> {
    
    /**
     * 添加任务评论
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param content 评论内容
     * @return 评论DTO
     */
    TaskCommentDTO addComment(Long taskId, Long userId, String content);
    
    /**
     * 删除任务评论
     * @param userId 用户ID (用于权限验证)
     * @param commentId 评论ID
     */
    void deleteComment(Long userId, Long commentId);
    
    /**
     * 获取任务的所有评论（按时间排序）
     * @param taskId 任务ID
     * @return 评论DTO列表
     */
    List<TaskCommentDTO> getCommentsByTaskId(Long taskId);
}
