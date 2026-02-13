-- Optimize project-team-competition relationship
-- Version: V1.2.0
-- Date: 2026-02-11

-- 1. Add competition_id to projects table
ALTER TABLE projects 
ADD COLUMN competition_id BIGINT NULL,
ADD INDEX idx_competition (competition_id);

-- 2. Remove UNIQUE constraint on teams.project_id if exists
ALTER TABLE teams DROP INDEX project_id;

-- 3. Optimize teams table fields
-- 3.1 Rename type to team_nature
ALTER TABLE teams 
CHANGE COLUMN type team_nature VARCHAR(32) DEFAULT 'TEMPORARY';

-- 3.2 Add status field
ALTER TABLE teams 
ADD COLUMN status VARCHAR(32) DEFAULT 'ACTIVE',
ADD INDEX idx_status (status);

-- 3.3 Add source_project_id field
ALTER TABLE teams 
ADD COLUMN source_project_id BIGINT NULL,
ADD INDEX idx_source_project (source_project_id);

-- 4. Create team_projects association table
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

-- 5. Migrate existing team-project relationships to team_projects table
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

-- 6. Update source_project_id for teams
UPDATE teams t
SET t.source_project_id = t.project_id
WHERE t.project_id IS NOT NULL;

-- 7. Update team_nature based on competition_id
UPDATE teams t
SET t.team_nature = CASE 
    WHEN t.competition_id IS NOT NULL THEN 'LONG_TERM'
    ELSE 'TEMPORARY'
END;

-- 8. Ensure all teams have status
UPDATE teams 
SET status = 'ACTIVE' 
WHERE status IS NULL;
