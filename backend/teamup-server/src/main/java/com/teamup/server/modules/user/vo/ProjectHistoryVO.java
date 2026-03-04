package com.teamup.server.modules.user.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目履历VO
 */
@Data
public class ProjectHistoryVO {
    
    private Long id;
    private Long projectId;
    private String projectTitle;
    private String projectType;
    private String projectDescription;
    
    private String role;  // LEADER/MEMBER
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;
    private Integer durationDays;
    
    // 评价数据
    private BigDecimal avgTechScore;
    private BigDecimal avgCollaborationScore;
    private BigDecimal avgTaskCompletionScore;
    private Integer evaluationCount;
    
    // 综合评分（1-5）
    private BigDecimal avgScore;
    
    // 可信度标记
    private Boolean isVerified;
    private String verificationSource;
}
