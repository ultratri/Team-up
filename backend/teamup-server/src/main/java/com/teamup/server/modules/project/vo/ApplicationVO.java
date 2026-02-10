package com.teamup.server.modules.project.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 申请详情VO
 */
@Data
public class ApplicationVO {
    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long applicantId;
    private String applicantName;
    private String applicantAvatar;
    private List<String> applicantSkills;
    private String applicationReason;
    private String status;
    private Long reviewedBy;
    private String reviewerName;
    private String reviewComment;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}
