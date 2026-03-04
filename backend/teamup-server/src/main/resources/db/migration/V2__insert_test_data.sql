-- =====================================================
-- 匹配功能测试数据
-- 创建时间: 2026-02-25
-- =====================================================

-- 插入项目技能需求测试数据
INSERT INTO project_skill_requirements (project_id, skill_name, is_required, expected_level) VALUES
(1, 'Java', TRUE, 'INTERMEDIATE'),
(1, 'Spring Boot', TRUE, 'INTERMEDIATE'),
(1, 'MySQL', FALSE, 'BEGINNER'),
(2, 'Vue.js', TRUE, 'INTERMEDIATE'),
(2, 'TypeScript', TRUE, 'INTERMEDIATE'),
(2, 'Element Plus', FALSE, 'BEGINNER');

-- 插入用户兴趣标签测试数据
INSERT INTO user_interests (user_id, interest_type, interest_name) VALUES
(1, 'RESEARCH', 'Web开发'),
(1, 'RESEARCH', '后端开发'),
(1, 'RESEARCH', '微服务架构'),
(2, 'RESEARCH', 'Web开发'),
(2, 'STARTUP', '前端开发'),
(2, 'STARTUP', 'UI设计');

-- 插入用户可用时间测试数据
-- 用户1：周一到周五晚上7点到10点
INSERT INTO user_availability (user_id, day_of_week, start_time, end_time) VALUES
(1, 1, '19:00:00', '22:00:00'),
(1, 2, '19:00:00', '22:00:00'),
(1, 3, '19:00:00', '22:00:00'),
(1, 4, '19:00:00', '22:00:00'),
(1, 5, '19:00:00', '22:00:00');

-- 用户2：周末全天
INSERT INTO user_availability (user_id, day_of_week, start_time, end_time) VALUES
(2, 6, '09:00:00', '18:00:00'),
(2, 7, '09:00:00', '18:00:00');

-- 插入协作历史测试数据
INSERT INTO collaboration_history (user_id, partner_id, project_id, collaboration_score) VALUES
(1, 2, 1, 0.85),
(2, 1, 1, 0.90);
