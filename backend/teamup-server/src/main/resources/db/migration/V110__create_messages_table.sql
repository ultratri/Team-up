-- 创建团队聊天消息表
-- 先删除旧表（如果存在）
DROP TABLE IF EXISTS messages;

-- 创建新的消息表
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    sender_name VARCHAR(100) NOT NULL COMMENT '发送者名称',
    sender_avatar VARCHAR(500) COMMENT '发送者头像URL',
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT, IMAGE, FILE, SYSTEM',
    content TEXT COMMENT '消息内容',
    file_url VARCHAR(500) COMMENT '文件URL',
    file_name VARCHAR(255) COMMENT '文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    mentioned_users TEXT COMMENT '被@的用户ID列表(JSON格式)',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_team_id (team_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队聊天消息表';
