-- 为 users 表添加 nickname 和 avatar 字段

ALTER TABLE users
ADD COLUMN nickname VARCHAR(100) COMMENT '昵称' AFTER phone,
ADD COLUMN avatar VARCHAR(500) COMMENT '头像URL' AFTER nickname;

-- 为现有用户设置默认昵称（使用 username）
UPDATE users SET nickname = username WHERE nickname IS NULL;
