-- 团队邀请表
CREATE TABLE IF NOT EXISTS team_invitations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    inviter_id BIGINT NOT NULL COMMENT '邀请人ID',
    invitee_id BIGINT NOT NULL COMMENT '被邀请人ID',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理, ACCEPTED-已接受, REJECTED-已拒绝, EXPIRED-已过期',
    message TEXT COMMENT '邀请留言',
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    responded_at TIMESTAMP NULL COMMENT '响应时间',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_team_id (team_id),
    INDEX idx_invitee_id (invitee_id),
    INDEX idx_status (status),
    INDEX idx_inviter_id (inviter_id),
    UNIQUE KEY uk_team_invitee (team_id, invitee_id),
    
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队邀请表';
