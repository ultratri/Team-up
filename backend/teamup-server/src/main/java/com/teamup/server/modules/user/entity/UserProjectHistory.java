package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户项目履历实体
 * 系统自动生成的可验证项目经验记录
 */
@Data
@TableName("user_project_history")
public class UserProjectHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long projectId;
    private Long teamId;
    
    // 基础信息
    private String role;  // LEADER/MEMBER
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    
    // 项目完成信息
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private Integer durationDays;
    
    // 评价数据（自动聚合）
    private BigDecimal avgTechScore;
    private BigDecimal avgCollaborationScore;
    private BigDecimal avgTaskCompletionScore;
    private Integer evaluationCount;
    
    // 项目信息快照
    private String projectTitle;
    private String projectType;
    private String projectDescription;
    
    // 可信度标记
    private Boolean isVerified;
    private String verificationSource;  // SYSTEM/MANUAL/IMPORT
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
