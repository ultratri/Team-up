-- 创建学员评价导师表
CREATE TABLE IF NOT EXISTS mentor_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    mentor_id BIGINT NOT NULL COMMENT '导师ID',
    student_id BIGINT NOT NULL COMMENT '学员ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    
    -- 评分维度（1-5分）
    professional_ability TINYINT NOT NULL COMMENT '专业能力评分(1-5)',
    guidance_attitude TINYINT NOT NULL COMMENT '指导态度评分(1-5)',
    response_speed TINYINT NOT NULL COMMENT '响应速度评分(1-5)',
    helpfulness TINYINT NOT NULL COMMENT '帮助程度评分(1-5)',
    
    -- 综合评分
    overall_rating DECIMAL(3,2) NOT NULL COMMENT '综合评分(1.00-5.00)',
    
    -- 文字评价
    comment TEXT COMMENT '文字评价',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-有效, DELETED-已删除',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_mentor_id (mentor_id),
    INDEX idx_student_id (student_id),
    INDEX idx_team_id (team_id),
    INDEX idx_created_at (created_at),
    
    -- 唯一约束：每个学员对每个导师在每个团队只能评价一次
    UNIQUE KEY uk_student_mentor_team (student_id, mentor_id, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员评价导师表';
