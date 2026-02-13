-- 人才墙性能优化：添加索引

-- 1. user_availability 表优化
-- 添加复合索引以优化可见范围和上墙状态的联合查询
CREATE INDEX idx_available_visibility ON user_availability(is_available, visibility);

-- 添加意向字段的全文索引以优化意向筛选
-- 注意：MySQL 的 LIKE 查询在有前缀时可以使用普通索引
CREATE INDEX idx_intention ON user_availability(intention);

-- 2. user_profiles 表优化
-- 添加院系索引以优化院系筛选（如果已存在会报错，可忽略）
CREATE INDEX idx_department ON user_profiles(department);

-- 添加用户ID索引以优化 JOIN 查询（如果已存在会报错，可忽略）
CREATE INDEX idx_user_id_profile ON user_profiles(user_id);

-- 3. user_credits 表优化
-- 添加用户ID索引以优化 JOIN 查询（如果已存在会报错，可忽略）
CREATE INDEX idx_user_id_credit ON user_credits(user_id);

-- 添加信誉分索引以优化排序
CREATE INDEX idx_total_credit ON user_credits(total_credit DESC);

-- 4. users 表优化
-- 添加状态索引以优化状态筛选（如果已存在会报错，可忽略）
CREATE INDEX idx_status ON users(status);

-- 添加最后登录时间索引以优化排序（如果已存在会报错，可忽略）
CREATE INDEX idx_last_login_at ON users(last_login_at DESC);

-- 5. user_tags 表优化
-- 添加复合索引以优化技能标签查询（如果已存在会报错，可忽略）
CREATE INDEX idx_user_tag_type ON user_tags(user_id, tag_type);

-- 6. projects 表优化
-- 添加创建者ID索引以优化项目创建者判断（如果已存在会报错，可忽略）
CREATE INDEX idx_creator_id ON projects(creator_id);

-- 7. user_roles 表优化
-- 添加复合索引以优化角色查询（如果已存在会报错，可忽略）
CREATE INDEX idx_user_role ON user_roles(user_id, role_name);

