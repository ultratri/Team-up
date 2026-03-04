package com.teamup.server.modules.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectApplication;

import java.util.List;
import java.util.Map;

/**
 * 项目服务接口
 */
public interface ProjectService {
    /**
     * 分页查询项目列表（项目大厅 - 所有公开项目，排除草稿）
     */
    Page<Project> getProjectList(int page, int size, String type, String status, String keyword, Long userId);

    /**
     * 分页查询我的项目列表（只返回当前用户创建的项目）
     */
    Page<Project> getMyProjectList(int page, int size, String type, String status, String keyword, Long userId);

    /**
     * 获取项目详情
     */
    Project getProjectById(Long id);
    
    /**
     * 增加项目浏览次数（带防刷机制）
     * 同一用户/IP 在 24 小时内只计数一次
     */
    void incrementProjectViews(Long projectId, Long userId, String ipAddress);

    /**
     * 创建项目
     */
    Project createProject(Project project, Long userId, List<Map<String, Object>> skillRequirements, List<Map<String, Object>> timeSlots);

    /**
     * 更新项目
     */
    void updateProject(Long id, Project project, Long userId, List<Map<String, Object>> skillRequirements, List<Map<String, Object>> timeSlots);

    /**
     * 删除项目
     */
    void deleteProject(Long id, Long userId);

    /**
     * 发布项目（从草稿到招募中）
     */
    void publishProject(Long id, Long userId);

    /**
     * 申请加入项目
     */
    ProjectApplication applyProject(Long projectId, Long userId, String reason);

    /**
     * 审核申请
     */
    void reviewApplication(Long applicationId, Long userId, boolean approved, String comment);

    /**
     * 获取我创建的项目的所有申请
     */
    Page<ProjectApplication> getMyProjectApplications(Long userId, int page, int size, String status);

    /**
     * 获取我的申请历史
     */
    Page<ProjectApplication> getMyApplications(Long userId, int page, int size);

    /**
     * 批量审核申请
     */
    void batchReviewApplications(List<Long> applicationIds, Long userId, boolean approved, String comment);

    /**
     * 获取项目推荐列表
     */
    List<Map<String, Object>> getProjectRecommendations(Long projectId, Long userId);
    
    /**
     * 完成项目（并处理团队：保留或解散）
     */
    void completeProject(Long projectId, Long userId, String teamAction, String summary);
    
    /**
     * 为项目创建或关联团队
     */
    void associateTeamWithProject(Long projectId, Long teamId, Long userId);

    /**
     * 获取项目技能需求列表
     */
    List<com.teamup.server.modules.project.entity.ProjectSkillRequirement> getProjectSkillRequirements(Long projectId);

    /**
     * 获取项目时间段需求列表
     */
    List<com.teamup.server.modules.project.entity.ProjectTimeSlot> getProjectTimeSlots(Long projectId);

}

