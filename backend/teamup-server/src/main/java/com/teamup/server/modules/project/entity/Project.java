package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目实体
 */
@Data
@TableName("projects")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long creatorId;
    
    @TableField(exist = false)  // 不映射到数据库字段
    private String creatorName;  // 创建者用户名（用于前端显示）
    
    private String title;
    private String projectType;  // COMPETITION, RESEARCH, STARTUP, OPENSOURCE, OTHER
    private String description;
    private String requirements;
    private Integer teamSize;
    private Integer currentMembers;
    private Integer expectedDuration;
    private Integer weeklyHours;
    private String expectedOutcome;
    private String status;  // DRAFT, RECRUITING, IN_PROGRESS, PENDING_REVIEW, COMPLETED, ARCHIVED
    private Boolean isRecommended;
    private Integer views;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long competitionId;  // 关联的比赛ID（可选）
    private Long teamId;  // 执行团队ID（招募完成后生成）
    private String teamMode;  // 团队模式：CREATE_NEW-创建新团队, USE_EXISTING-使用已有团队
}

