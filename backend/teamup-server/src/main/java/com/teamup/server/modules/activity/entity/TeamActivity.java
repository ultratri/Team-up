package com.teamup.server.modules.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队活动记录实体
 */
@Data
@TableName("team_activities")
public class TeamActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String activityType;  // task, file, message, member, setting
    private String action;
    private String detail;
    private Long relatedId;
    private LocalDateTime createdAt;
}
