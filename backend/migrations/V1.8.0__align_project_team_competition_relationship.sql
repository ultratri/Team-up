-- V1.8.0: 对齐项目-团队-比赛三者关系
-- 根据核心业务逻辑文档调整数据模型
-- Date: 2026-02-24

-- ============================================
-- 1. 优化 projects 表
-- ============================================

-- 1.1 确保 competition_id 字段存在（项目可选关联比赛）
ALTER TABLE projects 
ADD COLUMN IF NOT EXISTS competition_id BIGINT NULL COMMENT '关联的比赛ID（可选）',
ADD INDEX IF NOT EXISTS idx_competition (competition_id);

-- 1.2 添加 team_id 字段（招募完成后关联团队）
ALTER TABLE projects 
ADD COLUMN IF NOT EXISTS team_id BIGINT NULL COMMENT '执行团队ID（招募完成后生成）',
ADD INDEX IF NOT EXISTS idx_team (team_id);

-- 1.3 添加项目创建模式字段
ALTER TABLE projects 
ADD COLUMN IF NOT EXISTS team_mode VARCHAR(32) DEFAULT 'CREATE_NEW' COMMENT '团队模式：CREATE_NEW-创建新团队, USE_EXISTING-使用已有团队',
ADD INDEX IF NOT EXISTS idx_team_mode (team_mode);


-- ============================================
-- 2. 优化 teams 表
-- ============================================

-- 2.1 确保 team_nature 字段存在（团队性质）
-- 已在 V1.2.0 中创建，这里确保存在
ALTER TABLE teams 
MODIFY COLUMN team_nature VARCHAR(32) DEFAULT 'TEMPORARY' COMMENT '团队性质：TEMPORARY-临时, LONG_TERM-长期';

-- 2.2 确保 status 字段存在（团队状态）
ALTER TABLE teams 
MODIFY COLUMN status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '团队状态：ACTIVE-活跃, INACTIVE-不活跃, DISSOLVED-已解散';

-- 2.3 确保 source_project_id 字段存在（来源项目）
ALTER TABLE teams 
MODIFY COLUMN source_project_id BIGINT NULL COMMENT '来源项目ID（从哪个项目创建的团队）';

-- 2.4 移除 project_id 字段的唯一约束（如果存在）
-- 一个团队可以执行多个项目，通过 team_projects 表关联
ALTER TABLE teams DROP INDEX IF EXISTS uk_project_id;
ALTER TABLE teams DROP INDEX IF EXISTS project_id;


-- ============================================
-- 3. 确保 team_projects 关联表存在且结构正确
-- ============================================

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


-- ============================================
-- 4. 确保 team_competitions 关联表存在
-- ============================================

CREATE TABLE IF NOT EXISTS team_competitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    competition_id BIGINT NOT NULL COMMENT '比赛ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    UNIQUE KEY uk_team_competition (team_id, competition_id) COMMENT '团队和比赛的唯一约束',
    KEY idx_team_id (team_id) COMMENT '团队ID索引',
    KEY idx_competition_id (competition_id) COMMENT '比赛ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队比赛关联表';


-- ============================================
-- 5. 数据迁移：同步现有数据
-- ============================================

-- 5.1 将 teams.project_id 的数据迁移到 team_projects 表
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
WHERE t.project_id IS NOT NULL
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    started_at = VALUES(started_at);

-- 5.2 将 teams.competition_id 的数据迁移到 team_competitions 表
INSERT INTO team_competitions (team_id, competition_id, created_at)
SELECT t.id, t.competition_id, t.created_at
FROM teams t
WHERE t.competition_id IS NOT NULL
ON DUPLICATE KEY UPDATE 
    created_at = VALUES(created_at);

-- 5.3 更新 projects.team_id（反向关联）
UPDATE projects p
INNER JOIN teams t ON t.project_id = p.id
SET p.team_id = t.id
WHERE p.team_id IS NULL;

-- 5.4 更新 teams.source_project_id（如果还没有设置）
UPDATE teams t
SET t.source_project_id = t.project_id
WHERE t.project_id IS NOT NULL AND t.source_project_id IS NULL;


-- ============================================
-- 6. 设置默认值
-- ============================================

-- 6.1 确保所有团队都有状态
UPDATE teams 
SET status = 'ACTIVE' 
WHERE status IS NULL;

-- 6.2 确保所有团队都有性质
UPDATE teams 
SET team_nature = CASE 
    WHEN competition_id IS NOT NULL THEN 'LONG_TERM'
    ELSE 'TEMPORARY'
END
WHERE team_nature IS NULL;

-- 6.3 确保所有项目都有团队模式
UPDATE projects 
SET team_mode = 'CREATE_NEW' 
WHERE team_mode IS NULL;


-- ============================================
-- 7. 添加注释说明
-- ============================================

-- 为关键字段添加注释
ALTER TABLE projects 
MODIFY COLUMN competition_id BIGINT NULL COMMENT '关联的比赛ID（可选，项目可以参加比赛）',
MODIFY COLUMN team_id BIGINT NULL COMMENT '执行团队ID（招募完成后生成或选择已有团队）',
MODIFY COLUMN team_mode VARCHAR(32) DEFAULT 'CREATE_NEW' COMMENT '团队模式：CREATE_NEW-创建新团队, USE_EXISTING-使用已有团队';

ALTER TABLE teams 
MODIFY COLUMN team_nature VARCHAR(32) DEFAULT 'TEMPORARY' COMMENT '团队性质：TEMPORARY-临时团队（项目结束后可解散）, LONG_TERM-长期团队（可接多个项目）',
MODIFY COLUMN status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '团队状态：ACTIVE-活跃, INACTIVE-不活跃, DISSOLVED-已解散',
MODIFY COLUMN source_project_id BIGINT NULL COMMENT '来源项目ID（记录团队从哪个项目创建）',
MODIFY COLUMN project_id BIGINT NULL COMMENT '【已废弃】项目ID（保留用于兼容，新逻辑使用 team_projects 表）',
MODIFY COLUMN competition_id BIGINT NULL COMMENT '【已废弃】比赛ID（保留用于兼容，新逻辑使用 team_competitions 表）';


-- ============================================
-- 8. 创建视图方便查询
-- ============================================

-- 8.1 团队当前项目视图（显示团队正在进行的项目）
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

-- 8.2 项目团队视图（显示项目的执行团队）
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

-- 8.3 团队比赛视图（显示团队参加的比赛）
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


-- ============================================
-- 9. 数据完整性检查（可选）
-- ============================================

-- 检查是否有项目没有团队（招募中的项目可以没有团队）
-- SELECT id, title, status FROM projects WHERE team_id IS NULL AND status NOT IN ('DRAFT', 'RECRUITING');

-- 检查是否有团队没有项目（长期团队可以暂时没有项目）
-- SELECT id, team_name, team_nature, status FROM teams WHERE id NOT IN (SELECT team_id FROM team_projects WHERE status = 'IN_PROGRESS');

-- 检查是否有孤立的 team_projects 记录
-- SELECT * FROM team_projects WHERE team_id NOT IN (SELECT id FROM teams) OR project_id NOT IN (SELECT id FROM projects);
