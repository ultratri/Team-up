package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private Long competitionId;  // 关联的比赛ID
}

