SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

INSERT INTO resources (title, description, type, cover, content, author_id, views, likes, created_at, updated_at) VALUES
('Spring Boot 开发指南', '全面的 Spring Boot 开发教程，适合初学者和进阶开发者', 'TUTORIAL', 'https://via.placeholder.com/400x300', '# Spring Boot 开发指南\n\n本教程将带你从零开始学习 Spring Boot...', 1, 120, 45, NOW(), NOW()),
('Vue3 组件库', '基于 Vue3 和 TypeScript 的企业级组件库', 'LIBRARY', 'https://via.placeholder.com/400x300', '# Vue3 组件库\n\n提供丰富的企业级组件...', 2, 89, 32, NOW(), NOW()),
('微服务架构设计模式', '深入理解微服务架构的设计模式和最佳实践', 'ARTICLE', 'https://via.placeholder.com/400x300', '# 微服务架构设计模式\n\n微服务架构已经成为现代应用开发的主流...', 1, 156, 67, NOW(), NOW()),
('Python 数据分析工具包', '用于数据分析和可视化的 Python 工具集', 'TOOL', 'https://via.placeholder.com/400x300', '# Python 数据分析工具包\n\n包含数据清洗、分析和可视化功能...', 3, 78, 28, NOW(), NOW());

INSERT INTO resource_tags (resource_id, tag_name) VALUES
(LAST_INSERT_ID() - 3, 'Spring Boot'),
(LAST_INSERT_ID() - 3, 'Java'),
(LAST_INSERT_ID() - 3, '后端开发'),
(LAST_INSERT_ID() - 2, 'Vue3'),
(LAST_INSERT_ID() - 2, 'TypeScript'),
(LAST_INSERT_ID() - 2, '前端开发'),
(LAST_INSERT_ID() - 1, '微服务'),
(LAST_INSERT_ID() - 1, '架构设计'),
(LAST_INSERT_ID() - 1, '最佳实践'),
(LAST_INSERT_ID(), 'Python'),
(LAST_INSERT_ID(), '数据分析'),
(LAST_INSERT_ID(), '可视化');

INSERT INTO moments (user_id, type, content, related_project_id, likes, comments, created_at) VALUES
(1, 'PROJECT_UPDATE', '我们的项目刚刚完成了第一个里程碑！感谢团队的努力', 1, 15, 3, NOW()),
(2, 'ACHIEVEMENT', '今天成功通过了项目答辩，获得了优秀评级！', NULL, 23, 5, NOW()),
(3, 'SHARE', '分享一个很棒的开源项目，对我们的开发很有帮助', NULL, 8, 2, NOW()),
(1, 'TEAM_ACTIVITY', '团队今天进行了技术分享会，学到了很多新知识', 2, 12, 4, NOW()),
(2, 'PROJECT_UPDATE', '项目进展顺利，前端界面已经完成 80%', 3, 18, 6, NOW());
