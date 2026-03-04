-- 创建通知表
-- 用于支持举报处理结果通知和其他系统通知

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知类型：SYSTEM-系统, REPORT-举报, TEAM-团队, PROJECT-项目',
    related_type VARCHAR(20) NULL COMMENT '关联类型：TEAM-团队, PROJECT-项目, USER-用户, COMPETITION-比赛等',
    related_id BIGINT NULL COMMENT '关联ID',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_at DATETIME NULL COMMENT '阅读时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_user_unread (user_id, is_read),
    INDEX idx_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
