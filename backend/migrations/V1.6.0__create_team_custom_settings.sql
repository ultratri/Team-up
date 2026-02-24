-- V1.6.0: 创建团队自定义设置表
-- 支持自定义快捷入口、分组、团队信息等高级配置

-- 1. 创建团队自定义配置表
CREATE TABLE IF NOT EXISTS team_custom_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    
    -- 自定义快捷入口（JSON 数组）
    shortcuts_json JSON COMMENT '自定义快捷入口配置',
    
    -- 自定义分组（JSON 数组）
    groups_json JSON COMMENT '自定义工具分组配置',
    
    -- 团队首页信息
    team_announcement TEXT COMMENT '团队公告（支持 Markdown）',
    team_guidelines_json JSON COMMENT '常用规范链接',
    onboarding_checklist_json JSON COMMENT '新人入队指引',
    
    -- 权限配置
    shortcuts_edit_permission VARCHAR(20) DEFAULT 'leader' COMMENT '快捷入口编辑权限：leader/all',
    announcement_edit_permission VARCHAR(20) DEFAULT 'leader' COMMENT '公告编辑权限：leader/all',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_team (team_id),
    INDEX idx_team (team_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队自定义配置表';

-- 2. 插入默认配置示例（可选）
-- 为现有团队创建默认配置
INSERT INTO team_custom_config (team_id, shortcuts_json, groups_json)
SELECT 
    id,
    JSON_ARRAY() as shortcuts_json,
    JSON_ARRAY(
        JSON_OBJECT(
            'id', 'dev',
            'name', '研发工具',
            'order', 1,
            'links', JSON_ARRAY()
        ),
        JSON_OBJECT(
            'id', 'collab',
            'name', '协作工具',
            'order', 2,
            'links', JSON_ARRAY()
        ),
        JSON_OBJECT(
            'id', 'design',
            'name', '设计工具',
            'order', 3,
            'links', JSON_ARRAY()
        )
    ) as groups_json
FROM teams
WHERE NOT EXISTS (
    SELECT 1 FROM team_custom_config WHERE team_custom_config.team_id = teams.id
);

-- 3. 添加索引优化查询
CREATE INDEX idx_created_at ON team_custom_config(created_at);
CREATE INDEX idx_updated_at ON team_custom_config(updated_at);
