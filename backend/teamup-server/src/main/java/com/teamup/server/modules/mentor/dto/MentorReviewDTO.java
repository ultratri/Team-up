package com.teamup.server.modules.mentor.dto;

import lombok.Data;

/**
 * 学员评价导师DTO
 */
@Data
public class MentorReviewDTO {
    
    /**
     * 导师ID
     */
    private Long mentorId;
    
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
     * 文字评价（可选）
     */
    private String comment;
}
