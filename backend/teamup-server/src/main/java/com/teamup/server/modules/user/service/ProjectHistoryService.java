package com.teamup.server.modules.user.service;

import com.teamup.server.modules.user.dto.ExperienceScore;
import com.teamup.server.modules.user.entity.UserProjectHistory;
import java.util.List;

/**
 * 项目履历服务接口
 */
public interface ProjectHistoryService {
    
    /**
     * 获取用户的项目履历列表
     * @param userId 用户ID
     * @param onlyCompleted 是否只返回已完成的项目
     * @return 项目履历列表
     */
    List<UserProjectHistory> getUserProjectHistory(Long userId, boolean onlyCompleted);
    
    /**
     * 当项目完成时，自动创建/更新所有参与成员的履历
     * @param projectId 项目ID
     */
    void onProjectCompleted(Long projectId);
    
    /**
     * 当用户加入团队时，创建履历记录
     * @param userId 用户ID
     * @param teamId 团队ID
     * @param projectId 项目ID
     * @param role 角色
     */
    void onUserJoinedTeam(Long userId, Long teamId, Long projectId, String role);
    
    /**
     * 当收到评价时，更新履历中的评分数据
     * @param projectId 项目ID
     * @param evaluatedUserId 被评价用户ID
     */
    void onEvaluationReceived(Long projectId, Long evaluatedUserId);
    
    /**
     * 同步所有项目履历（定时任务）
     * 更新评价分数、计算参与天数等
     */
    void syncAllProjectHistory();
    
    /**
     * 计算用户的经验分数（用于匹配算法）
     * @param userId 用户ID
     * @return 经验分数对象
     */
    ExperienceScore calculateExperienceScore(Long userId);
}
