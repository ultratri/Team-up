-- V1.5.0: Create team_competitions table
-- Support multiple competitions per team

-- Create team_competitions association table
CREATE TABLE IF NOT EXISTS team_competitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    team_id BIGINT NOT NULL COMMENT 'Team ID',
    competition_id BIGINT NOT NULL COMMENT 'Competition ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Association time',
    UNIQUE KEY uk_team_competition (team_id, competition_id) COMMENT 'Unique constraint for team and competition',
    KEY idx_team_id (team_id) COMMENT 'Team ID index',
    KEY idx_competition_id (competition_id) COMMENT 'Competition ID index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team competition association table';

-- Migrate existing competition_id data from teams table to team_competitions table
INSERT INTO team_competitions (team_id, competition_id, created_at)
SELECT t.id, t.competition_id, t.created_at
FROM teams t
WHERE t.competition_id IS NOT NULL
ON DUPLICATE KEY UPDATE team_competitions.created_at = team_competitions.created_at;

-- Note: Keep competition_id field in teams table for backward compatibility
-- Can be removed in future versions if needed
