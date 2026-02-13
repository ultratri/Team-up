package com.teamup.server.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 人才VO（用于人才墙展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TalentVO {
    
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
     * 头像URL
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
     * 信誉分
     */
    private Integer creditScore;
    
    /**
     * 技能标签列表
     */
    private List<String> skills;
    
    /**
     * 组队意向列表
     */
    private List<String> intentions;
    
    /**
     * 每周可投入小时数
     */
    private Integer weeklyHours;
    
    /**
     * 备注说明
     */
    private String notes;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * 账号状态
     */
    private String status;
    
    /**
     * 可见范围
     */
    private String visibility;
    
    /**
     * 可用开始时间
     */
    private String availableFrom;
    
    /**
     * 可用结束时间
     */
    private String availableUntil;
    
    /**
     * 项目经验
     */
    private String projectExperience;
    
    /**
     * 邮箱（根据权限显示）
     */
    private String email;
    
    /**
     * 电话（根据权限显示）
     */
    private String phone;
    
    /**
     * 微信（根据权限显示）
     */
    private String wechat;
    
    /**
     * QQ（根据权限显示）
     */
    private String qq;
}
