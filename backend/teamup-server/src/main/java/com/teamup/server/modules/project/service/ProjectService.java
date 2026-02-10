package com.teamup.server.modules.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectApplication;

/**
 * 项目服务接口
 */
public interface ProjectService {
    /**
     * 分页查询项目列表（只返回当前用户创建的项目）
     */
    Page<Project> getProjectList(int page, int size, String type, String status, String keyword, Long userId);

    /**
     * 获取项目详情
     */
    Project getProjectById(Long id);

    /**
     * 创建项目
     */
    Project createProject(Project project, Long userId);

    /**
     * 更新项目
     */
    void updateProject(Long id, Project project, Long userId);

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
    void batchReviewApplications(java.util.List<Long> applicationIds, Long userId, boolean approved, String comment);

    /**
     * 获取项目推荐列表
     */
    java.util.List<java.util.Map<String, Object>> getProjectRecommendations(Long projectId, Long userId);
}

