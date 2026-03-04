package com.teamup.server.modules.project.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 项目创建请求DTO
 */
@Data
public class ProjectCreateRequest {
    
    /**
     * 项目标题
     */
    private String title;
    
    /**
     * 项目类型：COMPETITION-比赛, RESEARCH-科研, STARTUP-创业, OPENSOURCE-开源, OTHER-其他
     */
    private String projectType;
    
    /**
     * 项目描述
     */
    private String description;
    
    /**
     * 项目需求
     */
    private String requirements;
    
    /**
     * 团队规模
     */
    private Integer teamSize;
    
    /**
     * 预期持续时间（天）
     */
    private Integer expectedDuration;
    
    /**
     * 每周投入时间（小时）
     */
    private Integer weeklyHours;
    
    /**
     * 预期成果
     */
    private String expectedOutcome;
    
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
    
    /**
     * 关联的比赛ID（可选）
     */
    private Long competitionId;
    
    /**
     * 团队模式：CREATE_NEW-创建新团队, USE_EXISTING-使用已有团队
     */
    private String teamMode;
    
    /**
     * 已有团队ID（当 teamMode=USE_EXISTING 时必填）
     */
    private Long existingTeamId;
    
    /**
     * 团队名称（当 teamMode=CREATE_NEW 时使用）
     */
    private String teamName;
    
    /**
     * 是否邀请队友
     */
    private Boolean inviteTeammates;
    
    /**
     * 被邀请的用户ID列表
     */
    private java.util.List<Long> invitedUserIds;
}
