package com.teamup.server.modules.mentor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 导师详情VO
 */
@Data
public class MentorDetailVO {
    
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 院系
     */
    private String department;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 总学员数
     */
    private Integer totalMentees;
    
    /**
     * 当前活跃学员数
     */
    private Integer activeMentees;
    
    /**
     * 已完成学员数
     */
    private Integer completedMentees;
    
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
    
    /**
     * 成为导师时间
     */
    private LocalDateTime becameMentorAt;
    
    /**
     * 申请理由
     */
    private String applicationReason;
}
