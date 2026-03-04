package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队申请实体
 */
@Data
@TableName("team_applications")
public class TeamApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 项目ID
     */
    private Long projectId;
    
    /**
     * 发起人ID
     */
    private Long leaderId;
    
    /**
     * 申请说明
     */
    private String message;
    
    /**
     * 申请状态：PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消
     */
    private String status;
    
    /**
     * 审核人ID
     */
    private Long reviewedBy;
    
    /**
     * 审核意见
     */
    private String reviewComment;
    
    /**
     * 申请时间
     */
    private LocalDateTime appliedAt;
    
    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
