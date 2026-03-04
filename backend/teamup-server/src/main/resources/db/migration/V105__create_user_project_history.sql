-- 用户项目履历表（系统自动生成，可验证）
CREATE TABLE IF NOT EXISTS user_project_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    
    -- 基础信息
    role VARCHAR(50) NOT NULL COMMENT '角色：LEADER/MEMBER',
    joined_at TIMESTAMP NOT NULL COMMENT '加入时间',
    left_at TIMESTAMP NULL COMMENT '离开时间（NULL表示仍在项目中）',
    
    -- 项目完成信息
    is_completed BOOLEAN DEFAULT FALSE COMMENT '项目是否完成',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    duration_days INT COMMENT '参与天数',
    
    -- 评价数据（自动聚合from evaluations表）
    avg_tech_score DECIMAL(3,2) COMMENT '平均技术贡献分（1-5）',
    avg_collaboration_score DECIMAL(3,2) COMMENT '平均协作分（1-5）',
    avg_task_completion_score DECIMAL(3,2) COMMENT '平均任务完成分（1-5）',
    evaluation_count INT DEFAULT 0 COMMENT '收到的评价数量',
    
    -- 项目信息快照（冗余存储，避免项目删除后丢失）
    project_title VARCHAR(200) COMMENT '项目标题',
    project_type VARCHAR(50) COMMENT '项目类型',
    project_description TEXT COMMENT '项目描述',
    
    -- 可信度标记
    is_verified BOOLEAN DEFAULT TRUE COMMENT '是否系统验证（TRUE=系统自动生成，FALSE=用户手动添加）',
    verification_source VARCHAR(50) DEFAULT 'SYSTEM' COMMENT '验证来源：SYSTEM/MANUAL/IMPORT',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_user_project (user_id, project_id) COMMENT '一个用户在一个项目中只有一条履历',
    KEY idx_user_id (user_id) COMMENT '按用户查询',
    KEY idx_project_id (project_id) COMMENT '按项目查询',
    KEY idx_team_id (team_id) COMMENT '按团队查询',
    KEY idx_completed (is_completed, completed_at) COMMENT '查询已完成项目',
    KEY idx_verified (is_verified) COMMENT '查询验证状态',
    KEY idx_role (role) COMMENT '按角色查询',
    
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户项目履历表（系统自动生成，可验证）';

-- 初始化历史数据：同步所有已完成的项目
INSERT INTO user_project_history (
    user_id, project_id, team_id, role, joined_at, 
    is_completed, completed_at, duration_days,
    project_title, project_type, project_description,
    is_verified, verification_source
)
SELECT 
    tm.user_id,
    tp.project_id,
    tm.team_id,
    tm.role,
    tm.joined_at,
    TRUE as is_completed,
    tp.completed_at,
    DATEDIFF(COALESCE(tp.completed_at, NOW()), tm.joined_at) as duration_days,
    p.title as project_title,
    p.project_type,
    p.description as project_description,
    TRUE as is_verified,
    'SYSTEM' as verification_source
FROM team_members tm
JOIN team_projects tp ON tm.team_id = tp.team_id
JOIN projects p ON tp.project_id = p.id
WHERE tp.status = 'COMPLETED' 
  OR p.status = 'COMPLETED'
ON DUPLICATE KEY UPDATE
    is_completed = TRUE,
    completed_at = VALUES(completed_at),
    duration_days = VALUES(duration_days),
    updated_at = CURRENT_TIMESTAMP;

-- 更新评价数据
UPDATE user_project_history h
SET 
    avg_tech_score = (
        SELECT AVG(tech_contribution_score)
        FROM evaluations e
        WHERE e.project_id = h.project_id 
        AND e.evaluated_id = h.user_id
    ),
    avg_collaboration_score = (
        SELECT AVG(collaboration_score)
        FROM evaluations e
        WHERE e.project_id = h.project_id 
        AND e.evaluated_id = h.user_id
    ),
    avg_task_completion_score = (
        SELECT AVG(task_completion_score)
        FROM evaluations e
        WHERE e.project_id = h.project_id 
        AND e.evaluated_id = h.user_id
    ),
    evaluation_count = (
        SELECT COUNT(*)
        FROM evaluations e
        WHERE e.project_id = h.project_id 
        AND e.evaluated_id = h.user_id
    )
WHERE is_completed = TRUE;
