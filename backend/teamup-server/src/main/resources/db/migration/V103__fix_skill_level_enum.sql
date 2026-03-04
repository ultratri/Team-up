-- 修复技能等级枚举值，添加 ADVANCED
-- 创建时间: 2026-02-28

-- 修改 project_skill_requirements 表的 expected_level 字段
-- 从 ENUM('BEGINNER','INTERMEDIATE','EXPERT') 改为 ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT')
ALTER TABLE project_skill_requirements 
MODIFY COLUMN expected_level ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT') 
COMMENT '要求的熟练度等级';

-- 同时修改字段名，从 is_required 改为更规范的 required（如果需要的话）
-- 注意：这里保持 is_required 不变，因为实体类已经通过 @TableField 映射了
