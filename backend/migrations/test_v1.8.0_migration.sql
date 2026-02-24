-- 测试 V1.8.0 迁移脚本
-- 用于验证项目-团队-比赛关系调整是否正确

USE teamup;

-- ============================================
-- 1. 检查表结构
-- ============================================

SHOW COLUMNS FROM projects LIKE 'team_id';
SHOW COLUMNS FROM projects LIKE 'team_mode';
SHOW COLUMNS FROM projects LIKE 'competition_id';

SHOW COLUMNS FROM teams LIKE 'team_nature';
SHOW COLUMNS FROM teams LIKE 'status';
SHOW COLUMNS FROM teams LIKE 'source_project_id';

-- ============================================
-- 2. 检查关联表
-- ============================================

SELECT COUNT(*) AS team_projects_count FROM team_projects;
SELECT COUNT(*) AS team_competitions_count FROM team_competitions;

-- ============================================
-- 3. 检查视图
-- ============================================

SHOW FULL TABLES WHERE Table_type = 'VIEW';

-- ============================================
-- 4. 测试数据插入
-- ============================================

-- 4.1 创建测试项目（关联比赛）
INSERT INTO projects (
    creator_id, title, project_type, description, 
    team_size, status, competition_id, team_mode,
    created_at, updated_at
) VALUES (
    1, '测试项目-关联比赛', 'COMPETITION', '这是一个测试项目',
    5, 'DRAFT', 1, 'CREATE_NEW',
    NOW(), NOW()
);

SET @test_project_id = LAST_INSERT_ID();
SELECT CONCAT('创建测试项目，ID: ', @test_project_id) AS result;

-- 4.2 创建测试团队
INSERT INTO teams (
    team_name, leader_id, team_nature, status,
    source_project_id, created_at, updated_at
) VALUES (
    '测试团队', 1, 'TEMPORARY', 'ACTIVE',
    @test_project_id, NOW(), NOW()
);

SET @test_team_id = LAST_INSERT_ID();
SELECT CONCAT('创建测试团队，ID: ', @test_team_id) AS result;

-- 4.3 关联团队和项目
INSERT INTO team_projects (team_id, project_id, status, started_at)
VALUES (@test_team_id, @test_project_id, 'IN_PROGRESS', NOW());

SELECT CONCAT('关联团队和项目成功') AS result;

-- 4.4 关联团队和比赛
INSERT INTO team_competitions (team_id, competition_id, created_at)
VALUES (@test_team_id, 1, NOW());

SELECT CONCAT('关联团队和比赛成功') AS result;

-- ============================================
-- 5. 测试查询视图
-- ============================================

-- 5.1 查询团队当前项目
SELECT '=== 团队当前项目 ===' AS section;
SELECT * FROM v_team_current_projects WHERE team_id = @test_team_id;

-- 5.2 查询项目团队
SELECT '=== 项目团队 ===' AS section;
SELECT * FROM v_project_teams WHERE project_id = @test_project_id;

-- 5.3 查询团队比赛
SELECT '=== 团队比赛 ===' AS section;
SELECT * FROM v_team_competitions WHERE team_id = @test_team_id;

-- ============================================
-- 6. 测试团队执行多个项目
-- ============================================

-- 6.1 创建第二个项目
INSERT INTO projects (
    creator_id, title, project_type, description,
    team_size, status, team_mode,
    created_at, updated_at
) VALUES (
    1, '测试项目2-同一团队', 'RESEARCH', '同一团队的第二个项目',
    5, 'DRAFT', 'USE_EXISTING',
    NOW(), NOW()
);

SET @test_project_id_2 = LAST_INSERT_ID();

-- 6.2 关联同一团队
INSERT INTO team_projects (team_id, project_id, status, started_at)
VALUES (@test_team_id, @test_project_id_2, 'IN_PROGRESS', NOW());

-- 6.3 查询团队的所有项目
SELECT '=== 团队的所有项目 ===' AS section;
SELECT 
    tp.team_id,
    tp.project_id,
    p.title,
    tp.status,
    tp.started_at
FROM team_projects tp
INNER JOIN projects p ON tp.project_id = p.id
WHERE tp.team_id = @test_team_id;

-- ============================================
-- 7. 测试项目完成流程
-- ============================================

-- 7.1 完成第一个项目
UPDATE team_projects 
SET status = 'COMPLETED', completed_at = NOW()
WHERE team_id = @test_team_id AND project_id = @test_project_id;

UPDATE projects 
SET status = 'COMPLETED', updated_at = NOW()
WHERE id = @test_project_id;

-- 7.2 将团队转为长期团队
UPDATE teams 
SET team_nature = 'LONG_TERM', updated_at = NOW()
WHERE id = @test_team_id;

SELECT '=== 项目完成后的状态 ===' AS section;
SELECT 
    t.id AS team_id,
    t.team_name,
    t.team_nature,
    t.status,
    COUNT(tp.id) AS total_projects,
    SUM(CASE WHEN tp.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_projects,
    SUM(CASE WHEN tp.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS active_projects
FROM teams t
LEFT JOIN team_projects tp ON t.id = tp.team_id
WHERE t.id = @test_team_id
GROUP BY t.id;

-- ============================================
-- 8. 清理测试数据
-- ============================================

SELECT '=== 清理测试数据 ===' AS section;

DELETE FROM team_competitions WHERE team_id = @test_team_id;
DELETE FROM team_projects WHERE team_id = @test_team_id;
DELETE FROM teams WHERE id = @test_team_id;
DELETE FROM projects WHERE id IN (@test_project_id, @test_project_id_2);

SELECT '测试数据已清理' AS result;

-- ============================================
-- 9. 数据完整性检查
-- ============================================

SELECT '=== 数据完整性检查 ===' AS section;

-- 检查是否有孤立的 team_projects 记录
SELECT COUNT(*) AS orphan_team_projects
FROM team_projects tp
WHERE tp.team_id NOT IN (SELECT id FROM teams)
   OR tp.project_id NOT IN (SELECT id FROM projects);

-- 检查是否有孤立的 team_competitions 记录
SELECT COUNT(*) AS orphan_team_competitions
FROM team_competitions tc
WHERE tc.team_id NOT IN (SELECT id FROM teams)
   OR tc.competition_id NOT IN (SELECT id FROM competitions);

-- 检查项目状态与团队项目状态的一致性
SELECT 
    p.id AS project_id,
    p.title,
    p.status AS project_status,
    tp.status AS team_project_status
FROM projects p
LEFT JOIN team_projects tp ON p.id = tp.project_id
WHERE p.status IN ('COMPLETED', 'ARCHIVED')
  AND tp.status != 'COMPLETED'
  AND tp.id IS NOT NULL;

SELECT '=== 测试完成 ===' AS section;
