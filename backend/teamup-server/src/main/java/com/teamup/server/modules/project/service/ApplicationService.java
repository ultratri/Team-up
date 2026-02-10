package com.teamup.server.modules.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.project.entity.ProjectApplication;
import com.teamup.server.modules.project.vo.ApplicationVO;

import java.util.List;

/**
 * 项目申请管理服务接口
 */
public interface ApplicationService {
    /**
     * 创建申请
     */
    ProjectApplication createApplication(Long projectId, Long userId, String reason);
    
    /**
     * 审批申请
     */
    void reviewApplication(Long applicationId, Long reviewerId, boolean approved, String comment);
    
    /**
     * 批量审批申请
     */
    void batchReviewApplications(List<Long> applicationIds, Long reviewerId, boolean approved, String comment);
    
    /**
     * 撤回申请
     */
    void withdrawApplication(Long applicationId, Long userId);
    
    /**
     * 获取项目的所有申请
     */
    Page<ApplicationVO> getProjectApplications(Long projectId, int page, int size, String status);
    
    /**
     * 获取用户的所有申请
     */
    Page<ApplicationVO> getUserApplications(Long userId, int page, int size, String status);
    
    /**
     * 获取待处理申请数量
     */
    Long getPendingCount(Long projectId);
    
    /**
     * 获取申请详情
     */
    ApplicationVO getApplicationDetail(Long applicationId);
}
