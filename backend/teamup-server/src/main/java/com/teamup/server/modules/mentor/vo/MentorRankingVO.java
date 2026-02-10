package com.teamup.server.modules.mentor.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 导师排行VO
 */
@Data
public class MentorRankingVO {
    
    /**
     * 排名
     */
    private Integer rank;
    
    /**
     * 导师ID
     */
    private Long mentorId;
    
    /**
     * 导师姓名
     */
    private String mentorName;
    
    /**
     * 院系
     */
    private String department;
    
    /**
     * 成功培养学员数
     */
    private Integer successfulMentees;
    
    /**
     * 学员平均信誉分
     */
    private BigDecimal averageMenteeScore;
    
    /**
     * 累计奖励积分
     */
    private Integer totalRewardPoints;
    
    /**
     * 导师评分
     */
    private BigDecimal rating;
}
