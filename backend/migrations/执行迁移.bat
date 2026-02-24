@echo off
chcp 65001 >nul
echo ========================================
echo 项目-团队-比赛关系调整 - 数据库迁移
echo ========================================
echo.

echo [1/3] 连接数据库并执行迁移脚本...
mysql -u root -p123456 teamup < V1.8.0__align_project_team_competition_relationship.sql

if %errorlevel% neq 0 (
    echo.
    echo [错误] 迁移脚本执行失败！
    echo 请检查：
    echo 1. MySQL 服务是否启动
    echo 2. 数据库密码是否正确（默认：123456）
    echo 3. teamup 数据库是否存在
    pause
    exit /b 1
)

echo [成功] 迁移脚本执行完成！
echo.

echo [2/3] 执行测试脚本...
mysql -u root -p123456 teamup < test_v1.8.0_migration.sql

if %errorlevel% neq 0 (
    echo.
    echo [警告] 测试脚本执行失败，但迁移已完成
    pause
    exit /b 0
)

echo [成功] 测试脚本执行完成！
echo.

echo [3/3] 验证迁移结果...
mysql -u root -p123456 -e "USE teamup; SELECT '检查 projects 表新增字段' AS step; SHOW COLUMNS FROM projects LIKE 'team_id'; SHOW COLUMNS FROM projects LIKE 'team_mode';"

echo.
echo ========================================
echo 迁移完成！
echo ========================================
echo.
echo 接下来的步骤：
echo 1. 重启后端服务
echo 2. 查看文档：docs/项目团队比赛关系调整说明.md
echo 3. 适配前端页面
echo.
pause
