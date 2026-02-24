# 数据库迁移脚本使用说明

## V1.8.0 - 项目团队比赛关系调整

### 快速开始（Windows）

#### 1. 执行迁移

双击运行 `执行迁移.bat`，或在命令行中执行：

```cmd
cd backend\migrations
执行迁移.bat
```

脚本会自动：
- 执行数据库迁移
- 运行测试验证
- 显示迁移结果

#### 2. 验证迁移

双击运行 `验证迁移.bat`，或在命令行中执行：

```cmd
验证迁移.bat
```

#### 3. 回滚迁移（如需要）

双击运行 `回滚迁移.bat`，或在命令行中执行：

```cmd
回滚迁移.bat
```

**注意**：回滚操作需要输入 `YES` 确认。

### 手动执行（适用于所有平台）

#### Linux/Mac

```bash
# 执行迁移
mysql -u root -p123456 teamup < V1.8.0__align_project_team_competition_relationship.sql

# 执行测试
mysql -u root -p123456 teamup < test_v1.8.0_migration.sql

# 验证结果
mysql -u root -p123456 -e "USE teamup; SHOW COLUMNS FROM projects LIKE 'team_id';"
```

#### Windows (PowerShell)

```powershell
# 执行迁移
Get-Content V1.8.0__align_project_team_competition_relationship.sql | mysql -u root -p123456 teamup

# 执行测试
Get-Content test_v1.8.0_migration.sql | mysql -u root -p123456 teamup
```

### 迁移内容

本次迁移包含以下内容：

1. **表结构调整**
   - `projects` 表新增 `team_id` 和 `team_mode` 字段
   - `teams` 表优化字段注释和约束
   - 确保 `team_projects` 和 `team_competitions` 关联表存在

2. **数据迁移**
   - 将现有的团队-项目关系迁移到 `team_projects` 表
   - 将现有的团队-比赛关系迁移到 `team_competitions` 表

3. **视图创建**
   - `v_team_current_projects` - 团队当前项目视图
   - `v_project_teams` - 项目团队视图
   - `v_team_competitions` - 团队比赛视图

### 验证检查项

迁移完成后，请检查以下内容：

1. ✅ `projects` 表是否有 `team_id` 和 `team_mode` 字段
2. ✅ `team_projects` 表是否有数据
3. ✅ `team_competitions` 表是否有数据
4. ✅ 三个视图是否创建成功
5. ✅ 没有孤立的关联记录

### 常见问题

#### Q1: 执行迁移时提示"Access denied"
**A**: 检查数据库密码是否正确（默认：123456）

#### Q2: 执行迁移时提示"Unknown database 'teamup'"
**A**: 确保 teamup 数据库已创建

```sql
CREATE DATABASE IF NOT EXISTS teamup CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### Q3: 迁移执行失败，如何回滚？
**A**: 运行 `回滚迁移.bat` 或手动执行回滚 SQL

#### Q4: 如何备份数据库？
**A**: 在执行迁移前，建议先备份：

```bash
mysqldump -u root -p123456 teamup > teamup_backup_$(date +%Y%m%d).sql
```

Windows:
```cmd
mysqldump -u root -p123456 teamup > teamup_backup_%date:~0,4%%date:~5,2%%date:~8,2%.sql
```

### 相关文档

- [项目团队比赛关系调整说明](../../docs/项目团队比赛关系调整说明.md)
- [调整完成清单](../../docs/调整完成清单.md)
- [核心业务逻辑深度分析与解决方案](../../docs/Main/核心业务逻辑深度分析与解决方案.md)

### 技术支持

如遇到问题，请：
1. 查看相关文档
2. 检查数据库日志
3. 联系开发团队
