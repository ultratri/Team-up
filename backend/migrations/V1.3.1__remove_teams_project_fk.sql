-- 移除 teams 表的 project_id 外键约束
-- 原因：比赛队伍不一定需要关联项目，外键约束导致创建队伍失败
-- Version: V1.3.1
-- Date: 2026-02-12

-- 移除外键约束
ALTER TABLE teams DROP FOREIGN KEY fk_teams_project;

-- 保留索引以提高查询性能
-- idx_project_id 索引已存在，无需重新创建
