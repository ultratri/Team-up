package com.teamup.server.modules.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private String type;  // SYSTEM, APPLICATION_REVIEWED, etc. (对应数据库的type字段)
    private String notificationType;  // APPLICATION_RESULT, TASK_ASSIGNED, etc. (对应数据库的notification_type字段)
    private String title;
    private String content;
    private String relatedType;  // PROJECT, TEAM, USER
    private Long relatedId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
