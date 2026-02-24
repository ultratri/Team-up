@echo off
chcp 65001 >nul
echo ========================================
echo 验证数据库迁移结果
echo ========================================
echo.

echo [检查1] 验证表结构...
mysql -u root -p123456 -e "USE teamup; SHOW COLUMNS FROM projects WHERE Field IN ('team_id', 'team_mode', 'competition_id');"
echo.

echo [检查2] 验证关联表...
mysql -u root -p123456 -e "USE teamup; SELECT 'team_projects' AS table_name, COUNT(*) AS record_count FROM team_projects UNION ALL SELECT 'team_competitions', COUNT(*) FROM team_competitions;"
echo.

echo [检查3] 验证视图...
mysql -u root -p123456 -e "USE teamup; SHOW FULL TABLES WHERE Table_type = 'VIEW';"
echo.

echo [检查4] 数据完整性检查...
mysql -u root -p123456 -e "USE teamup; SELECT COUNT(*) AS orphan_team_projects FROM team_projects tp WHERE tp.team_id NOT IN (SELECT id FROM teams) OR tp.project_id NOT IN (SELECT id FROM projects);"
echo.

echo ========================================
echo 验证完成！
echo ========================================
pause
