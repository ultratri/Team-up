package com.teamup.server.modules.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String messageType; // TEXT, IMAGE, FILE, SYSTEM
    private String content;
    
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    
    // JSON strings for simplification
    private String mentionedUsers; 
    
    private Boolean isRead;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
