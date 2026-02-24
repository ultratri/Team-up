USE team_matching;

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
