package com.teamup.server.modules.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话成员实体
 */
@Data
@TableName("conversation_members")
public class ConversationMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long conversationId;
    private Long userId;
    private Long unreadCount;        // 未读消息数
    private Long lastReadMessageId;  // 最后已读消息ID
    private Boolean isMuted;         // 是否免打扰
    private Boolean isPinned;        // 是否置顶
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
}
