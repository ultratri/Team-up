-- 重命名 student_id 为 user_code，统一用户编号命名
-- 迁移版本：V1.3.0
-- 创建时间：2026-02-11

-- 1. 重命名 users 表的 student_id 字段为 user_code
ALTER TABLE users 
CHANGE COLUMN student_id user_code VARCHAR(50) NOT NULL UNIQUE 
COMMENT '用户编号（学号/工号/账号等）';

-- 2. 重命名 mentor_applications 表的 employee_id 字段为 user_code
ALTER TABLE mentor_applications 
CHANGE COLUMN employee_id user_code VARCHAR(50) NOT NULL 
COMMENT '用户编号（工号）';

-- 验证修改
SELECT 'users 表字段已重命名为 user_code' AS message;
SELECT 'mentor_applications 表字段已重命名为 user_code' AS message;
