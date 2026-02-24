-- 清空现有的英文数据
TRUNCATE TABLE department_major;

-- 插入中文院系专业数据
-- 计算机学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('计算机学院', '计算机科学与技术', 1, 1),
('计算机学院', '软件工程', 2, 1),
('计算机学院', '网络工程', 3, 1),
('计算机学院', '信息安全', 4, 1),
('计算机学院', '人工智能', 5, 1),
('计算机学院', '数据科学与大数据技术', 6, 1);

-- 信息工程学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('信息工程学院', '电子信息工程', 1, 1),
('信息工程学院', '通信工程', 2, 1),
('信息工程学院', '自动化', 3, 1),
('信息工程学院', '微电子科学与工程', 4, 1),
('信息工程学院', '物联网工程', 5, 1);

-- 数学与统计学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('数学与统计学院', '数学与应用数学', 1, 1),
('数学与统计学院', '信息与计算科学', 2, 1),
('数学与统计学院', '统计学', 3, 1);

-- 管理学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('管理学院', '信息管理与信息系统', 1, 1),
('管理学院', '工商管理', 2, 1),
('管理学院', '市场营销', 3, 1),
('管理学院', '会计学', 4, 1),
('管理学院', '财务管理', 5, 1);

-- 经济学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('经济学院', '经济学', 1, 1),
('经济学院', '国际经济与贸易', 2, 1),
('经济学院', '金融学', 3, 1);

-- 外国语学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('外国语学院', '英语', 1, 1),
('外国语学院', '日语', 2, 1),
('外国语学院', '翻译', 3, 1);

-- 设计与艺术学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('设计与艺术学院', '视觉传达设计', 1, 1),
('设计与艺术学院', '环境设计', 2, 1),
('设计与艺术学院', '产品设计', 3, 1),
('设计与艺术学院', '数字媒体艺术', 4, 1);

-- 机械与电气工程学院
INSERT INTO department_major (department, major, sort_order, enabled) VALUES
('机械与电气工程学院', '机械工程', 1, 1),
('机械与电气工程学院', '机械设计制造及其自动化', 2, 1),
('机械与电气工程学院', '电气工程及其自动化', 3, 1);
