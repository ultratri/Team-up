-- 用户时间段表（用于匹配功能）
CREATE TABLE IF NOT EXISTS user_time_slots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    day_of_week INT NOT NULL COMMENT '星期几(1-7,1表示周一)',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户时间段表（匹配功能）';

-- 插入测试数据
-- 用户1：周一到周五晚上7点到10点
INSERT INTO user_time_slots (user_id, day_of_week, start_time, end_time) VALUES
(1, 1, '19:00:00', '22:00:00'),
(1, 2, '19:00:00', '22:00:00'),
(1, 3, '19:00:00', '22:00:00'),
(1, 4, '19:00:00', '22:00:00'),
(1, 5, '19:00:00', '22:00:00');

-- 用户2：周末全天
INSERT INTO user_time_slots (user_id, day_of_week, start_time, end_time) VALUES
(2, 6, '09:00:00', '18:00:00'),
(2, 7, '09:00:00', '18:00:00');
