package com.teamup.server.modules.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告实体
 */
@Data
@TableName("announcements")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    private String content;
    private String priority;  // LOW, MEDIUM, HIGH
    
    @TableField("is_active")
    private Boolean isActive;
    
    @TableField("publisher_id")
    private Long publisherId;
    
    @TableField("published_at")
    private LocalDateTime publishedAt;
    
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
