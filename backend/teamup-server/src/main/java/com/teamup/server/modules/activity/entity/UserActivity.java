package com.teamup.server.modules.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户活动记录实体
 */
@Data
@TableName("user_activities")
public class UserActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private String activityType;  // LOGIN, VIEW_PROJECT, APPLY_PROJECT, CREATE_PROJECT, etc.
    private String description;
    private String ipAddress;
    private String userAgent;
    private String relatedType;  // PROJECT, TEAM, USER
    private Long relatedId;
    private LocalDateTime createdAt;
}
