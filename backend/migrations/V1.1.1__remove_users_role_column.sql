-- 删除users表的role字段
-- 原因：该字段与user_roles表冗余，且经常导致数据不一致
-- 统一使用user_roles表管理用户角色

ALTER TABLE users DROP COLUMN IF EXISTS role;

-- 说明：
-- 1. 所有角色信息现在统一存储在user_roles表中
-- 2. 支持一个用户拥有多个角色
-- 3. 查询用户角色请使用：SELECT role_name FROM user_roles WHERE user_id = ?
