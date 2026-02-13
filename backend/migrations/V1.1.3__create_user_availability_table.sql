-- 创建用户可用性/组队意向表

CREATE TABLE IF NOT EXISTS user_availability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    is_available TINYINT(1) DEFAULT 0 COMMENT '是否可用（上人才墙）',
    intention VARCHAR(100) COMMENT '意向（逗号分隔）：JOIN_PROJECT,FIND_TEAMMATES,FIND_MENTOR,HELP_NEWBIE',
    visibility VARCHAR(20) DEFAULT 'PUBLIC' COMMENT '可见范围：PUBLIC,PROJECT_CREATOR,MENTOR',
    available_from DATE COMMENT '可用开始时间',
    available_until DATE COMMENT '可用结束时间',
    weekly_hours INT COMMENT '每周可投入小时数',
    notes TEXT COMMENT '备注说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_is_available (is_available),
    INDEX idx_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户可用性/组队意向';
