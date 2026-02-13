-- Add missing fields to teams table and create team_projects table
-- Version: V1.2.1
-- Date: 2026-02-11

-- 1. Add status field to teams if not exists
ALTER TABLE teams 
ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'ACTIVE',
ADD INDEX IF NOT EXISTS idx_status (status);

-- 2. Add source_project_id field to teams if not exists
ALTER TABLE teams 
ADD COLUMN IF NOT EXISTS source_project_id BIGINT NULL,
ADD INDEX IF NOT EXISTS idx_source_project (source_project_id);

-- 3. Create team_projects table if not exists
CREATE TABLE IF NOT EXISTS team_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_project (team_id, project_id),
    INDEX idx_team (team_id),
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Migrate existing team-project relationships if team_projects is empty
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
AND NOT EXISTS (
    SELECT 1 FROM team_projects tp 
    WHERE tp.team_id = t.id AND tp.project_id = t.project_id
);

-- 5. Update source_project_id for teams that don't have it
UPDATE teams t
SET t.source_project_id = t.project_id
WHERE t.project_id IS NOT NULL AND t.source_project_id IS NULL;

-- 6. Ensure all teams have status
UPDATE teams 
SET status = 'ACTIVE' 
WHERE status IS NULL;
