package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团队邀请实体
 */
@Data
@TableName("team_invitations")
public class TeamInvitation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 团队ID
     */
    private Long teamId;
    
    /**
     * 邀请人ID
     */
    private Long inviterId;
    
    /**
     * 被邀请人ID
     */
    private Long inviteeId;
    
    /**
     * 状态：PENDING-待处理, ACCEPTED-已接受, REJECTED-已拒绝, EXPIRED-已过期
     */
    private String status;
    
    /**
     * 邀请留言
     */
    private String message;
    
    /**
     * 邀请时间
     */
    private LocalDateTime invitedAt;
    
    /**
     * 响应时间
     */
    private LocalDateTime respondedAt;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
