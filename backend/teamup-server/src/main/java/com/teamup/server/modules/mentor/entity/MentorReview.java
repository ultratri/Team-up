package com.teamup.server.modules.mentor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员评价导师实体
 */
@Data
@TableName("mentor_reviews")
public class MentorReview {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 导师ID
     */
    private Long mentorId;
    
    /**
     * 学员ID
     */
    private Long studentId;
    
    /**
     * 团队ID
     */
    private Long teamId;
    
    /**
     * 专业能力评分(1-5)
     */
    private Integer professionalAbility;
    
    /**
     * 指导态度评分(1-5)
     */
    private Integer guidanceAttitude;
    
    /**
     * 响应速度评分(1-5)
     */
    private Integer responseSpeed;
    
    /**
     * 帮助程度评分(1-5)
     */
    private Integer helpfulness;
    
    /**
     * 综合评分(1.00-5.00)
     */
    private BigDecimal overallRating;
    
    /**
     * 文字评价
     */
    private String comment;
    
    /**
     * 状态：ACTIVE-有效, DELETED-已删除
     */
    private String status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
