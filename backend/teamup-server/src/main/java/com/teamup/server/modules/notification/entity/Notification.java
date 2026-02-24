package com.teamup.server.modules.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
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
    
    @TableField("user_id")
    private Long userId;
    
    // type 字段：用于存储详细的通知类型（如 SYSTEM_ANNOUNCEMENT, TEAM_JOIN_APPLICATION 等）
    @TableField("type")
    private String type;
    
    // notificationType 字段：数据库的枚举字段（APPLICATION_RESULT, TASK_ASSIGNED 等）
    @TableField("notification_type")
    private String notificationType;
    
    private String title;
    private String content;
    
    @TableField("related_type")
    private String relatedType;  // PROJECT, TEAM, USER
    
    @TableField("related_id")
    private Long relatedId;
    
    @TableField("is_read")
    private Boolean isRead;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("read_at")
    private LocalDateTime readAt;
}
