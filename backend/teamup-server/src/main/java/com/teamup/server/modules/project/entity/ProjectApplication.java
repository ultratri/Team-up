package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目申请实体
 */
@Data
@TableName("project_applications")
public class ProjectApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    private Long applicantId;
    private String applicationReason;
    private String status;  // PENDING, APPROVED, REJECTED
    private Long reviewedBy;
    private String reviewComment;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}

