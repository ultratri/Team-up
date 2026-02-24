package com.teamup.server.modules.user.vo;

import lombok.Data;
import java.util.List;

/**
 * 人才墙展示对象
 */
@Data
public class TalentVO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 院系
     */
    private String department;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 年级
     */
    private Integer grade;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 微信
     */
    private String wechat;
    
    /**
     * QQ
     */
    private String qq;
    
    /**
     * 电话
     */
    private String phone;
    
    /**
     * 是否可用
     */
    private Boolean isAvailable;
    
    /**
     * 组队意向列表
     */
    private List<String> intentions;
    
    /**
     * 每周可用时间（小时）
     */
    private Integer weeklyHours;
    
    /**
     * 信誉分
     */
    private Integer creditScore;
}
