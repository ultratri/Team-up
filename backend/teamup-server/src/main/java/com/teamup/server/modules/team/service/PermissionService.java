package com.teamup.server.modules.team.service;

/**
 * 权限验证服务接口
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
public interface PermissionService {
    
    /**
     * 检查用户是否可以编辑任务
     * 规则：任务创建者或负责人可以编辑任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return true if user can edit, false otherwise
     */
    boolean canEditTask(Long userId, Long taskId);
    
    /**
     * 检查用户是否可以删除任务
     * 规则：团队管理员可以删除任何任务，普通成员只能删除自己创建的任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return true if user can delete, false otherwise
     */
    boolean canDeleteTask(Long userId, Long taskId);
    
    /**
     * 检查用户是否可以删除评论
     * 规则：只能删除自己的评论
     * 
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return true if user can delete comment, false otherwise
     */
    boolean canDeleteComment(Long userId, Long commentId);
    
    /**
     * 检查用户是否是团队成员
     * 
     * @param userId 用户ID
     * @param teamId 团队ID
     * @return true if user is team member, false otherwise
     */
    boolean isTeamMember(Long userId, Long teamId);
    
    /**
     * 检查用户是否是团队管理员
     * 
     * @param userId 用户ID
     * @param teamId 团队ID
     * @return true if user is team admin, false otherwise
     */
    boolean isTeamAdmin(Long userId, Long teamId);
}
