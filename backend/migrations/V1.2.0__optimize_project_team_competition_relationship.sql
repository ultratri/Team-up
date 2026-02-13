-- ============================================
-- 优化项目-团队-比赛关系
-- 版本：V1.2.0
-- 日期：2026-02-11
-- 说明：
--   1. 项目表添加比赛关联字段
--   2. 团队表优化字段设计，支持团队复用
--   3. 创建团队项目关联表，支持一个团队执行多个项目
-- ============================================

-- 1. 项目表添加比赛关联字段
ALTER TABLE projects 
ADD COLUMN competition_id BIGINT NULL COMMENT '关联的比赛ID（可选）',
ADD INDEX idx_competition (competition_id);

-- 2. 移除teams表的project_id UNIQUE约束（如果存在）
-- 注意：需要先检查约束名称
ALTER TABLE teams DROP INDEX project_id;

-- 3. 优化团队表字段
-- 3.1 修改type字段为team_nature
ALTER TABLE teams 
CHANGE COLUMN type team_nature VARCHAR(32) DEFAULT 'TEMPORARY' 
    COMMENT '团队性质：TEMPORARY-临时团队, LONG_TERM-长期团队';

-- 3.2 添加团队状态字段
ALTER TABLE teams 
ADD COLUMN status VARCHAR(32) DEFAULT 'ACTIVE' 
    COMMENT '团队状态：ACTIVE-活跃, DISBANDED-已解散',
ADD INDEX idx_status (status);

-- 3.3 添加来源项目字段
ALTER TABLE teams 
ADD COLUMN source_project_id BIGINT NULL 
    COMMENT '来源项目ID（记录团队最初从哪个项目创建）',
ADD INDEX idx_source_project (source_project_id);

-- 4. 创建团队项目关联表
CREATE TABLE IF NOT EXISTS team_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' 
        COMMENT '状态：IN_PROGRESS-进行中, COMPLETED-已完成',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_team_project (team_id, project_id),
    INDEX idx_team (team_id),
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队项目关联表';

-- 5. 数据迁移：将现有的team-project关系迁移到中间表
INSERT INTO team_projects (team_id, project_id, status, started_at)
SELECT 
    t.id AS team_id,
    t.project_id,
    CASE 
        WHEN p.status IN ('COMPLETED', 'ARCHIVED') THEN 'COMPLETED'
        ELSE 'IN_PROGRESS'
    END AS status,
    t.created_at AS started_at
FROM teams t
INNER JOIN projects p ON t.project_id = p.id
WHERE t.project_id IS NOT NULL;

-- 6. 更新团队的source_project_id
UPDATE teams t
SET t.source_project_id = t.project_id
WHERE t.project_id IS NOT NULL;

-- 7. 更新团队性质（根据competition_id判断）
-- 如果团队关联了比赛，默认设为长期团队
UPDATE teams t
SET t.team_nature = CASE 
    WHEN t.competition_id IS NOT NULL THEN 'LONG_TERM'
    ELSE 'TEMPORARY'
END;

-- 8. 确保所有团队都有状态
UPDATE teams 
SET status = 'ACTIVE' 
WHERE status IS NULL;
