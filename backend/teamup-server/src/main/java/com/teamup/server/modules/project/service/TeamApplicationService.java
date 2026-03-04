package com.teamup.server.modules.project.service;

import com.teamup.server.modules.project.dto.TeamApplicationDTO;
import com.teamup.server.modules.project.dto.TeamApplicationRequest;
import java.util.List;

/**
 * 团队申请服务接口
 */
public interface TeamApplicationService {
    
    /**
     * 创建团队申请
     * 
     * @param projectId 项目ID
     * @param leaderId 发起人ID
     * @param request 申请请求
     * @return 团队申请DTO
     */
    TeamApplicationDTO createTeamApplication(Long projectId, Long leaderId, TeamApplicationRequest request);
    
    /**
     * 获取团队申请详情
     * 
     * @param applicationId 申请ID
     * @return 团队申请DTO
     */
    TeamApplicationDTO getTeamApplication(Long applicationId);
    
    /**
     * 获取项目的所有团队申请
     * 
     * @param projectId 项目ID
     * @return 团队申请列表
     */
    List<TeamApplicationDTO> getProjectTeamApplications(Long projectId);
    
    /**
     * 获取用户的团队申请历史
     * 
     * @param userId 用户ID
     * @return 团队申请列表
     */
    List<TeamApplicationDTO> getUserTeamApplications(Long userId);
    
    /**
     * 成员确认参与团队申请
     * 
     * @param applicationId 申请ID
     * @param userId 用户ID
     */
    void confirmMembership(Long applicationId, Long userId);
    
    /**
     * 审核团队申请
     * 
     * @param applicationId 申请ID
     * @param reviewerId 审核人ID
     * @param approved 是否通过
     * @param comment 审核意见
     */
    void reviewTeamApplication(Long applicationId, Long reviewerId, boolean approved, String comment);
    
    /**
     * 取消团队申请
     * 
     * @param applicationId 申请ID
     * @param userId 用户ID（必须是发起人）
     */
    void cancelTeamApplication(Long applicationId, Long userId);
}
