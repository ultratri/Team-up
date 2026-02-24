package com.teamup.server.modules.mentor.vo;

import lombok.Data;

/**
 * 导师卡片VO（用于导师广场）
 */
@Data
public class MentorCardVO {
    
    /**
     * 用户ID
     */
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
     * 头像
     */
    private String avatar;
    
    /**
     * 头像URL（兼容字段）
     */
    private String avatarUrl;
    
    /**
     * 院系
     */
    private String department;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 项目经验
     */
    private String projectExperience;
    
    /**
     * 指导经验
     */
    private String guidanceExperience;
    
    /**
     * 总学员数
     */
    private Integer totalMentees;
    
    /**
     * 当前活跃学员数
     */
    private Integer activeMentees;
    
    /**
     * 成功培养学员数
     */
    private Integer successfulMentees;
    
    /**
     * 累计奖励积分
     */
    private Integer totalRewardPoints;
    
    /**
     * 导师评分（0-5）
     */
    private Double rating;
    
    /**
     * 是否可申请
     */
    private Boolean available;
    
    /**
     * 微信号
     */
    private String wechat;
    
    /**
     * QQ号
     */
    private String qq;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 电话
     */
    private String phone;
}
