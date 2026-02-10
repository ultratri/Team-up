# 校园组队系统 - Team Up Project

基于多维标签与加权匹配算法的校园组队平台

---

## 📚 项目文档

完整的项目文档位于 [`docs/`](./docs/) 目录：

- **[文档索引](./docs/README.md)** - 查看所有文档
- **[团队管理功能](./docs/team-management/README.md)** - 团队管理模块文档
- **[测试指南](./TESTING.md)** - 测试文档和运行指南 ⭐
- **[实现计划](./docs/implementation-tasks/IMPLEMENTATION_PLAN.md)** - 硬编码功能实现计划
- **[任务清单](./docs/implementation-tasks/TASK_CHECKLIST.md)** - 实现任务清单

---

## 🚀 快速开始

### 前置要求
- Node.js 16+
- Java 11+
- MySQL 8.0+
- Redis (可选)

### 后端启动
```bash
cd backend/teamup-server
mvn spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

### 数据库初始化
```bash
mysql -u root -p < backend/db_schema.sql
```

### 运行测试
```bash
# 后端管理员功能测试
cd tests/admin-smoke
.\run-admin-test.ps1

# 前端测试
cd frontend
npm run test
```

详细测试指南请查看 [TESTING.md](./TESTING.md)

---

## 📁 项目结构

```
team-up-project/
├── backend/                    # 后端服务
│   ├── teamup-server/         # Spring Boot 应用
│   └── db_schema.sql          # 数据库 Schema
├── frontend/                   # 前端应用
│   ├── src/                   # 源代码
│   └── tests/                 # 前端测试文件
├── tests/                      # 测试脚本目录 ⭐
│   └── admin-smoke/           # 管理员功能冒烟测试
└── docs/                      # 项目文档
    ├── testing/               # 测试文档 ⭐
    ├── team-management/       # 团队管理文档
    ├── fixes/                 # 问题修复文档
    ├── database/              # 数据库文档
    └── implementation-tasks/  # 实现任务文档
```

---

## ✨ 主要功能

### ✅ 已实现
- 用户注册、登录、个人资料管理
- 项目创建、浏览、申请
- 团队创建、成员管理
- 任务管理（看板）
- 实时聊天
- 通知系统

### 🚧 进行中
- 团队统计数据
- 团队活动记录
- 文件管理功能
- 团队成员评价

### 📋 计划中
- 智能匹配推荐
- 数据分析面板
- 移动端适配

---

## 🛠️ 技术栈

### 后端
- Spring Boot 2.7
- MyBatis-Plus
- MySQL 8.0
- Redis
- Socket.IO

### 前端
- Vue 3
- TypeScript
- Element Plus
- Pinia
- Vite

---

## 🧪 测试状态

### 后端测试
- ✅ 管理员功能冒烟测试: 14/14 通过 (100%)
- 📍 位置: `tests/admin-smoke/`

### 前端测试
- ✅ 单元测试: 84/84 通过 (100%)
- 📍 位置: `frontend/tests/`

详细信息请查看 [测试指南](./TESTING.md)

---

## 📖 开发指南

### 代码规范
- 后端：遵循阿里巴巴 Java 开发手册
- 前端：使用 ESLint + Prettier

### 提交规范
```
feat: 新功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试
chore: 构建/工具
```

### 分支管理
- `main` - 生产环境
- `develop` - 开发环境
- `feature/*` - 功能分支
- `fix/*` - 修复分支

---

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交代码
4. 创建 Pull Request

---

## 📄 许可证

本项目仅用于学习和研究目的。

---

## 📞 联系方式

- 项目文档：[docs/README.md](./docs/README.md)
- 问题反馈：通过 Issue 提交

---

**最后更新**: 2026-01-23
