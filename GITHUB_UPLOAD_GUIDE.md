# GitHub 上传指南

## 📋 准备工作清单

### 1. 已完成的配置
- ✅ `.gitignore` 已优化，排除了以下内容：
  - 编译文件（target/, dist/, build/）
  - IDE 配置（.idea/, .vscode/, .kiro/）
  - 依赖包（node_modules/, venv/）
  - 日志文件（*.log）
  - 环境配置（.env, application-prod.yml）
  - 文档和分析文件（project-files/, docs/）
  - 测试文件（frontend/tests/）
  - 临时文件（uploads/, redis/）

### 2. 需要检查的敏感信息
在上传前，请确认以下文件不包含敏感信息：
- `backend/teamup-server/src/main/resources/application.yml`
- 任何包含数据库密码、API 密钥的配置文件

---

## 🚀 上传步骤

### 步骤 1: 清理 Git 缓存
由于修改了 `.gitignore`，需要清理已追踪的文件：

```bash
# 清理所有已追踪但应该被忽略的文件
git rm -r --cached .
git add .
```

### 步骤 2: 提交当前更改
```bash
# 查看将要提交的文件
git status

# 添加所有更改
git add .

# 提交更改
git commit -m "chore: 优化 .gitignore，准备上传到 GitHub"
```

### 步骤 3: 创建 GitHub 仓库
1. 访问 https://github.com/new
2. 填写仓库信息：
   - Repository name: `team-up-project`（或你喜欢的名称）
   - Description: `基于多维技能标签与双向加权匹配算法的校园组队协作平台`
   - 选择 Public 或 Private
   - **不要**勾选 "Initialize with README"（因为本地已有）

### 步骤 4: 关联远程仓库
```bash
# 如果是新仓库，添加远程地址
git remote add origin https://github.com/你的用户名/team-up-project.git

# 如果已有 origin，可以修改
git remote set-url origin https://github.com/你的用户名/team-up-project.git

# 验证远程地址
git remote -v
```

### 步骤 5: 推送到 GitHub
```bash
# 首次推送（设置上游分支）
git push -u origin main

# 如果分支名是 master
git push -u origin master
```

---

## 📝 推送后的检查清单

### 1. 验证文件结构
访问 GitHub 仓库，确认以下内容：
- ✅ 源代码文件都已上传
- ✅ README.md 正确显示
- ✅ 没有 `node_modules/`, `target/`, `.idea/` 等目录
- ✅ 没有 `project-files/`, `docs/`, `uploads/` 等文档目录
- ✅ 没有敏感配置文件

### 2. 测试克隆
在另一个目录测试克隆：
```bash
git clone https://github.com/你的用户名/team-up-project.git
cd team-up-project
```

---

## 🔒 安全建议

### 1. 创建配置模板文件
为敏感配置创建示例文件：

```bash
# 后端配置模板
cp backend/teamup-server/src/main/resources/application.yml \
   backend/teamup-server/src/main/resources/application.yml.example

# 前端环境变量模板
cp frontend/.env frontend/.env.example
```

然后在模板文件中替换敏感信息为占位符：
```yaml
# application.yml.example
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/teamup
    username: your_username
    password: your_password
```

### 2. 添加 .env.example
在前端目录创建 `.env.example`：
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080
```

---

## 📦 后续维护

### 日常提交流程
```bash
# 1. 查看更改
git status

# 2. 添加更改
git add .

# 3. 提交（使用规范的提交信息）
git commit -m "feat: 添加新功能"
# 或
git commit -m "fix: 修复某个 bug"
# 或
git commit -m "docs: 更新文档"

# 4. 推送到 GitHub
git push
```

### 分支管理建议
```bash
# 创建开发分支
git checkout -b develop

# 功能开发
git checkout -b feature/新功能名称

# Bug 修复
git checkout -b fix/bug描述
```

---

## ❓ 常见问题

### Q1: 推送时提示文件过大
A: 检查是否有大文件未被 `.gitignore` 排除，使用以下命令查找：
```bash
find . -type f -size +10M
```

### Q2: 推送失败，提示 "rejected"
A: 可能是远程仓库有更新，先拉取：
```bash
git pull origin main --rebase
git push
```

### Q3: 如何删除已上传的敏感文件
A: 使用 BFG Repo-Cleaner 或 git filter-branch：
```bash
# 从历史记录中删除文件
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch 敏感文件路径" \
  --prune-empty --tag-name-filter cat -- --all

# 强制推送
git push origin --force --all
```

---

## 📞 需要帮助？

如果遇到问题，可以：
1. 查看 Git 官方文档：https://git-scm.com/doc
2. 查看 GitHub 帮助：https://docs.github.com
3. 检查错误信息并搜索解决方案

---

**祝上传顺利！** 🎉
