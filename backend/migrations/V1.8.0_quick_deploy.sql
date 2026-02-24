-- V1.8.0 快速部署脚本
-- 只添加缺失的字段和功能

USE team_matching;

-- 1. 为 projects 表添加 team_mode 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA='team_matching' AND TABLE_NAME='projects' AND COLUMN_NAME='team_mode');

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE projects ADD COLUMN team_mode VARCHAR(32) DEFAULT ''CREATE_NEW'' COMMENT ''团队模式：CREATE_NEW-创建新团队, USE_EXISTING-使用已有团队'', ADD INDEX idx_team_mode (team_mode);',
    'SELECT ''team_mode already exists'' AS message;');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 确保 team_projects 表存在
CREATE TABLE IF NOT EXISTS team_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS-进行中, COMPLETED-已完成',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_team_project (team_id, project_id) COMMENT '一个团队只能关联同一项目一次',
    INDEX idx_team (team_id),
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队项目关联表';

-- 3. 确保 team_competitions 表存在
CREATE TABLE IF NOT EXISTS team_competitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    competition_id BIGINT NOT NULL COMMENT '比赛ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    UNIQUE KEY uk_team_competition (team_id, competition_id) COMMENT '团队和比赛的唯一约束',
    KEY idx_team_id (team_id) COMMENT '团队ID索引',
    KEY idx_competition_id (competition_id) COMMENT '比赛ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队比赛关联表';

-- 4. 迁移现有数据到 team_projects（如果表是空的）
INSERT IGNORE INTO team_projects (team_id, project_id, status, started_at)
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

-- 5. 迁移现有数据到 team_competitions（如果表是空的）
INSERT IGNORE INTO team_competitions (team_id, competition_id, created_at)
SELECT t.id, t.competition_id, t.created_at
FROM teams t
WHERE t.competition_id IS NOT NULL;

-- 6. 创建视图
CREATE OR REPLACE VIEW v_team_current_projects AS
SELECT 
    tp.team_id,
    tp.project_id,
    p.title AS project_title,
    p.status AS project_status,
    tp.status AS team_project_status,
    tp.started_at,
    t.team_name,
    t.team_nature,
    t.status AS team_status
FROM team_projects tp
INNER JOIN projects p ON tp.project_id = p.id
INNER JOIN teams t ON tp.team_id = t.id
WHERE tp.status = 'IN_PROGRESS';

CREATE OR REPLACE VIEW v_project_teams AS
SELECT 
    p.id AS project_id,
    p.title AS project_title,
    p.status AS project_status,
    p.competition_id,
    t.id AS team_id,
    t.team_name,
    t.team_nature,
    t.status AS team_status,
    tp.status AS team_project_status,
    tp.started_at,
    tp.completed_at
FROM projects p
LEFT JOIN team_projects tp ON p.id = tp.project_id
LEFT JOIN teams t ON tp.team_id = t.id;

CREATE OR REPLACE VIEW v_team_competitions AS
SELECT 
    tc.team_id,
    tc.competition_id,
    t.team_name,
    t.team_nature,
    t.status AS team_status,
    c.name AS competition_name,
    c.status AS competition_status,
    tc.created_at AS joined_at
FROM team_competitions tc
INNER JOIN teams t ON tc.team_id = t.id
INNER JOIN competitions c ON tc.competition_id = c.id;

SELECT 'V1.8.0 快速部署完成！' AS message;
