@echo off
chcp 65001 >nul
echo ========================================
echo 回滚 V1.8.0 迁移（谨慎操作！）
echo ========================================
echo.
echo 警告：此操作将删除以下内容：
echo - projects 表的 team_id 和 team_mode 字段
echo - 视图 v_team_current_projects, v_project_teams, v_team_competitions
echo.
echo 注意：team_projects 和 team_competitions 表不会被删除
echo       因为它们可能包含重要的历史数据
echo.

set /p confirm="确定要回滚吗？(输入 YES 继续): "
if not "%confirm%"=="YES" (
    echo 已取消回滚操作
    pause
    exit /b 0
)

echo.
echo 正在回滚...

mysql -u root -p123456 teamup -e "DROP VIEW IF EXISTS v_team_current_projects; DROP VIEW IF EXISTS v_project_teams; DROP VIEW IF EXISTS v_team_competitions; ALTER TABLE projects DROP COLUMN IF EXISTS team_id; ALTER TABLE projects DROP COLUMN IF EXISTS team_mode;"

if %errorlevel% neq 0 (
    echo.
    echo [错误] 回滚失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo 回滚完成！
echo ========================================
echo.
echo 注意：
echo 1. team_projects 和 team_competitions 表仍然保留
echo 2. 如需完全清理，请手动执行：
echo    DROP TABLE team_projects;
echo    DROP TABLE team_competitions;
echo.
pause
