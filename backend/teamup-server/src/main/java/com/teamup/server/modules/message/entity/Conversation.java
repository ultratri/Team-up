package com.teamup.server.modules.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话实体
 */
@Data
@TableName("conversations")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("type")
    private String conversationType;  // PRIVATE(私聊), GROUP(群聊)
    private String name;              // 会话名称（群聊时使用）
    private String avatar;            // 会话头像（群聊时使用）
    private Long lastMessageId;       // 最后一条消息ID
    private LocalDateTime lastMessageTime;
    private LocalDateTime createdAt;
}
