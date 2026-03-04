-- 添加用户封禁相关字段
-- 用于支持举报系统的用户封禁功能

ALTER TABLE users
ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE-正常, BANNED-封禁',
ADD COLUMN ban_until DATETIME NULL COMMENT '封禁截止时间',
ADD COLUMN ban_reason VARCHAR(500) NULL COMMENT '封禁原因';

-- 添加索引以提升查询性能
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_ban_until ON users(ban_until);

-- 更新现有用户状态为正常
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;
