package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户档案实体
 */
@Data
@TableName("user_profiles")
public class UserProfile {
    @TableId(type = IdType.AUTO)
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
    private String guidanceExperience;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

