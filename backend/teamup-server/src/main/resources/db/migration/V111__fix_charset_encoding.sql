-- =====================================================
-- 修复数据库字符编码问题
-- 创建时间: 2026-03-04
-- 说明: 确保所有表和字段使用正确的 UTF-8 字符集
-- =====================================================

-- 修复 user_profiles 表中可能存在的乱码数据
-- 注意：这个脚本是幂等的，可以安全地多次执行

-- 1. 确保表使用正确的字符集
ALTER TABLE user_profiles CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 确保其他可能包含中文的表也使用正确的字符集
ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE projects CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE teams CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE competitions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE announcements CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 验证数据库默认字符集
-- 注意：这个命令在迁移脚本中不会执行，仅作为文档说明
-- ALTER DATABASE team_matching CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
