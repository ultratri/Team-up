package com.teamup.server.modules.user.vo;

import lombok.Data;

/**
 * 用户个人资料视图对象
 */
@Data
public class UserProfileVO {
    private Long id;
    private Long userId;
    private String realName;
    private String department;
    private String major;
    private Integer grade;
    private String avatarUrl;
    private String wechat;
    private String qq;
    private String bio;
    private String projectExperience;
}
