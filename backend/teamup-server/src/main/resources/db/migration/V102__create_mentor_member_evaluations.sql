-- =====================================================
-- 导师对团队成员的评价表
-- 创建时间: 2026-02-26
-- =====================================================

-- 导师成员评价表
CREATE TABLE IF NOT EXISTS mentor_member_evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    mentor_id BIGINT NOT NULL COMMENT '导师ID',
    member_id BIGINT NOT NULL COMMENT '被评价成员ID',
    score INT NOT NULL COMMENT '评分(0-100)',
    technical_ability INT COMMENT '技术能力评分(1-5)',
    collaboration INT COMMENT '协作能力评分(1-5)',
    learning_attitude INT COMMENT '学习态度评分(1-5)',
    task_completion INT COMMENT '任务完成度评分(1-5)',
    comment TEXT COMMENT '评价内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_team_id (team_id),
    INDEX idx_mentor_id (mentor_id),
    INDEX idx_member_id (member_id),
    UNIQUE KEY uk_mentor_team_member (mentor_id, team_id, member_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导师成员评价表';
