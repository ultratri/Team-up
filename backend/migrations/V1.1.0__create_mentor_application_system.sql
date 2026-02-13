-- 创建导师申请系统

-- 导师申请表
CREATE TABLE IF NOT EXISTS mentor_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人用户ID',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    employee_id VARCHAR(50) NOT NULL COMMENT '工号',
    department VARCHAR(100) COMMENT '院系',
    major VARCHAR(100) COMMENT '专业',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    bio TEXT COMMENT '个人简介',
    project_experience TEXT COMMENT '项目经验',
    guidance_experience TEXT COMMENT '指导经验',
    application_reason TEXT COMMENT '申请理由',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_comment TEXT COMMENT '审核意见',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导师申请表';

-- 清理旧数据：删除所有导师角色和绩效记录
DELETE FROM user_roles WHERE role_name = 'MENTOR';
DELETE FROM mentor_performance;
DELETE FROM mentor_relationships;

-- 确保所有用户都有STUDENT角色（如果没有任何角色）
INSERT INTO user_roles (user_id, role_name, granted_at)
SELECT u.id, 'STUDENT', NOW()
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.id IS NULL AND u.status = 'ACTIVE';
