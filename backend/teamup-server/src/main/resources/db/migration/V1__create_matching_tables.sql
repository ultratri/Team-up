-- =====================================================
-- 匹配功能相关数据库表
-- 创建时间: 2026-02-25
-- =====================================================

-- 项目技能需求表
CREATE TABLE IF NOT EXISTS project_skill_requirements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    skill_name VARCHAR(100) NOT NULL COMMENT '技能名称',
    required BOOLEAN DEFAULT FALSE COMMENT '是否必需技能',
    proficiency_level VARCHAR(20) COMMENT '要求的熟练度等级',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_project_id (project_id),
    INDEX idx_skill_name (skill_name),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目技能需求表';

-- 用户可用时间表
CREATE TABLE IF NOT EXISTS user_availability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    day_of_week INT NOT NULL COMMENT '星期几(1-7,1表示周一)',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户可用时间表';

-- 用户兴趣标签表
CREATE TABLE IF NOT EXISTS user_interests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    interest_name VARCHAR(100) NOT NULL COMMENT '兴趣名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_interest_name (interest_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_interest (user_id, interest_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣标签表';

-- 协作历史表（用于社交因素匹配）
CREATE TABLE IF NOT EXISTS collaboration_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    partner_id BIGINT NOT NULL COMMENT '合作伙伴ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    collaboration_score DECIMAL(3,2) DEFAULT 0.00 COMMENT '协作评分(0-1)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_partner_id (partner_id),
    INDEX idx_project_id (project_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (partner_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作历史表';

-- 匹配缓存表（存储预计算的匹配结果）
CREATE TABLE IF NOT EXISTS matching_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cache_key VARCHAR(255) NOT NULL COMMENT '缓存键',
    cache_type VARCHAR(50) NOT NULL COMMENT '缓存类型(USER_TO_PROJECT, PROJECT_TO_USER, TEAM_TO_PROJECT)',
    entity_id BIGINT NOT NULL COMMENT '实体ID（用户ID或项目ID或团队ID）',
    match_results JSON COMMENT '匹配结果JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    INDEX idx_cache_key (cache_key),
    INDEX idx_entity_id (entity_id),
    INDEX idx_expires_at (expires_at),
    UNIQUE KEY uk_cache_key (cache_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='匹配缓存表';
