-- 清理重复和废弃的表

-- 1. 删除废弃备份表
DROP TABLE IF EXISTS dept_admin_backup_20260204;

-- 2. 迁移skill_tags数据到tags表（如果有不重复的数据）
INSERT INTO tags (name, category, usage_count, is_official, created_at)
SELECT 
    tag_name as name,
    COALESCE(tag_category, 'SKILL') as category,
    usage_count,
    is_official,
    created_at
FROM skill_tags
WHERE tag_name NOT IN (SELECT name FROM tags);

-- 3. 删除skill_tags表
DROP TABLE IF EXISTS skill_tags;

-- 4. 删除user_skills表（空表，功能与user_tags重复）
DROP TABLE IF EXISTS user_skills;

-- 说明：
-- - skill_tags表与tags表功能重复，已合并到tags表
-- - user_skills表与user_tags表功能重复，且为空表，已删除
-- - 统一使用tags表和user_tags表管理所有标签
