-- 创建团队申请表
CREATE TABLE IF NOT EXISTS team_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '团队申请ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    leader_id BIGINT NOT NULL COMMENT '发起人ID',
    message TEXT COMMENT '申请说明',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消',
    reviewed_by BIGINT COMMENT '审核人ID',
    review_comment TEXT COMMENT '审核意见',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_project_id (project_id),
    INDEX idx_leader_id (leader_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队申请表';

-- 创建团队申请成员表
CREATE TABLE IF NOT EXISTS team_application_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员ID',
    team_application_id BIGINT NOT NULL COMMENT '团队申请ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    confirmed BOOLEAN DEFAULT FALSE COMMENT '是否确认参与',
    confirmed_at TIMESTAMP NULL COMMENT '确认时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (team_application_id) REFERENCES team_applications(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_team_application_id (team_application_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_team_application_user (team_application_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队申请成员表';
