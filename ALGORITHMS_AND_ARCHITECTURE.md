# 🧮 Team Up 核心算法与架构文档

本文档详细讲解 Team Up 项目的所有核心算法、系统架构和数据库设计。

## 📚 文档导航

### 1. 核心算法文档
详细讲解所有匹配算法、信誉评分算法、新手保护机制和推荐算法。

**文档位置**：`project-files/docs/ALGORITHMS.md`

**主要内容**：
- 🎯 智能匹配算法（成员找项目、项目招募成员、团队找项目）
- 📊 信誉评分算法（信誉分计算、等级划分）
- 🆕 新手保护机制（新手判定、加成、任务）
- 🔮 推荐算法（个性化推荐、热门推荐、协作推荐）

**关键公式**：
```
总匹配度 = 技能匹配(40%) + 时间匹配(25%) + 兴趣匹配(20%) + 比赛匹配(10%) + 新手加成(5%)
```

---

### 2. 系统架构文档
详细讲解系统的整体架构、后端分层、前端架构、实时通信和部署架构。

**文档位置**：`project-files/docs/ARCHITECTURE.md`

**主要内容**：
- 🏗️ 系统架构概览（整体架构图、核心模块交互）
- 🗄️ 数据库设计（核心表结构、ER 图）
- 🔧 后端架构（分层架构、模块划分、请求处理流程）
- 🎨 前端架构（目录结构、状态管理、组件通信）
- 🔌 实时通信架构（WebSocket 连接、事件类型）
- 🚀 部署架构（容器化、Kubernetes、CI/CD）

**架构图**：
```
用户浏览器 → API 网关 → 后端服务集群 → MySQL/Redis/匹配服务
```

---

### 3. 数据库 ER 图文档
详细讲解数据库设计、表关系、关键设计决策和索引策略。

**文档位置**：`project-files/docs/DATABASE_ER_DIAGRAM.md`

**主要内容**：
- 📊 完整 ER 图（核心实体、扩展实体）
- 🔗 表关系说明（一对多、多对多、自引用）
- 💡 关键设计决策（JSON 存储、ENUM、TIMESTAMP、索引、约束）

**核心表**：
- users（用户）
- projects（项目）
- teams（团队）
- project_applications（项目申请）
- team_members（团队成员）
- evaluations（评价）
- competitions（比赛）

---

## 🎯 快速查找

### 我想了解匹配算法
👉 查看 `ALGORITHMS.md` 的第 1 章 "智能匹配算法"

**包含内容**：
- 成员找项目的匹配公式和代码实现
- 项目招募成员的匹配公式
- 团队找项目的匹配公式
- 各维度的详细计算方法

### 我想了解信誉系统
👉 查看 `ALGORITHMS.md` 的第 2 章 "信誉评分算法"

**包含内容**：
- 信誉分计算公式
- 项目完成评价的影响
- 信誉等级划分
- 信誉分更新机制

### 我想了解新手保护
👉 查看 `ALGORITHMS.md` 的第 3 章 "新手保护机制"

**包含内容**：
- 新手判定条件
- 新手加成规则
- 新手任务和奖励
- 综合保护方案

### 我想了解系统架构
👉 查看 `ARCHITECTURE.md` 的第 1 章 "系统架构概览"

**包含内容**：
- 整体架构图
- 核心模块交互
- 请求处理流程

### 我想了解后端设计
👉 查看 `ARCHITECTURE.md` 的第 3 章 "后端架构"

**包含内容**：
- 分层架构（Controller → Service → Repository）
- 模块划分
- 请求处理流程

### 我想了解前端设计
👉 查看 `ARCHITECTURE.md` 的第 4 章 "前端架构"

**包含内容**：
- 目录结构
- Pinia 状态管理
- 组件通信方式

### 我想了解数据库设计
👉 查看 `DATABASE_ER_DIAGRAM.md`

**包含内容**：
- 完整 ER 图
- 表关系说明
- 关键设计决策
- 索引策略

---

## 📊 核心算法速查表

### 匹配算法权重

| 维度 | 权重 | 说明 |
|------|------|------|
| 技能匹配 | 40% | 用户技能与项目需求的匹配度 |
| 时间匹配 | 25% | 用户可用时间与项目需求的匹配度 |
| 兴趣匹配 | 20% | 用户兴趣与项目类型的相似度 |
| 比赛匹配 | 10% | 用户对项目关联比赛的兴趣 |
| 新手加成 | 5% | 新手用户的额外加成 |

### 信誉等级

| 信誉分 | 等级 | 徽章 |
|--------|------|------|
| 0-20 | 新手 | 🌱 |
| 21-40 | 初级 | 📚 |
| 41-60 | 中级 | ⭐ |
| 61-80 | 高级 | 🏆 |
| 81-100 | 大师 | 👑 |

### 新手保护条件

```
是否新手 = (注册时间 < 30天) AND (完成项目 < 3个)

新手加成 = 5分（在总匹配度中）
新手信誉加成 = 10分（初始信誉分 = 50 + 10 = 60分）
```

---

## 🏗️ 核心架构速查表

### 系统分层

```
Controller 层 (API 接口)
    ↓
Service 层 (业务逻辑)
    ↓
Repository 层 (数据访问)
    ↓
数据库 / 缓存
```

### 核心模块

| 模块 | 职责 | 关键类 |
|------|------|--------|
| user | 用户管理、认证、信誉 | UserController, UserService |
| project | 项目管理、匹配 | ProjectController, ProjectService |
| team | 团队管理、协作 | TeamController, TeamService |
| matching | 匹配算法调用 | MatchingController, MatchingService |
| chat | 实时聊天 | ChatEventHandler, ChatService |
| notification | 通知系统 | NotificationService |
| evaluation | 评估系统 | EvaluationController, EvaluationService |

### 前端状态管理

```
Pinia Store
├── auth store (认证状态)
├── user store (用户信息)
├── project store (项目列表)
├── team store (团队信息)
└── ...
```

---

## 📊 核心表速查表

### 用户相关表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| users | 用户 | id, username, email, credit_score |
| user_skills | 用户技能 | user_id, skill_name, proficiency_level |
| user_interests | 用户兴趣 | user_id, interest_name |
| user_availability | 用户可用时间 | user_id, available_hours |
| user_follows | 用户关注 | follower_id, following_id |

### 项目相关表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| projects | 项目 | id, creator_id, team_id, status |
| project_applications | 项目申请 | project_id, user_id, status, match_score |
| project_favorites | 项目收藏 | user_id, project_id |

### 团队相关表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| teams | 团队 | id, leader_id, type, status |
| team_members | 团队成员 | team_id, user_id, role |

### 其他表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| evaluations | 评价 | project_id, evaluator_id, evaluated_id |
| competitions | 比赛 | id, name, status |
| notifications | 通知 | recipient_id, type, is_read |
| messages | 消息 | sender_id, receiver_id, is_read |

---

## 🔗 相关文档链接

- [README.md](./README.md) - 项目概览和功能介绍
- [project-files/docs/Main/核心业务逻辑深度分析与解决方案.md](./project-files/docs/Main/核心业务逻辑深度分析与解决方案.md) - 业务逻辑分析
- [project-files/docs/ALGORITHMS.md](./project-files/docs/ALGORITHMS.md) - 详细算法文档
- [project-files/docs/ARCHITECTURE.md](./project-files/docs/ARCHITECTURE.md) - 详细架构文档
- [project-files/docs/DATABASE_ER_DIAGRAM.md](./project-files/docs/DATABASE_ER_DIAGRAM.md) - 详细数据库文档

---

## 💡 使用建议

### 对于新开发者
1. 先阅读 [README.md](./README.md) 了解项目概览
2. 再阅读 [ARCHITECTURE.md](./project-files/docs/ARCHITECTURE.md) 了解系统架构
3. 最后阅读相关模块的代码

### 对于算法工程师
1. 重点阅读 [ALGORITHMS.md](./project-files/docs/ALGORITHMS.md)
2. 查看 `matching-service/` 中的 Python 实现
3. 查看 `backend/teamup-server/src/main/java/com/teamup/server/modules/matching/` 中的 Java 实现

### 对于数据库工程师
1. 重点阅读 [DATABASE_ER_DIAGRAM.md](./project-files/docs/DATABASE_ER_DIAGRAM.md)
2. 查看 `backend/teamup-server/src/main/resources/db/migration/` 中的迁移脚本
3. 查看各个 Entity 类的定义

### 对于前端开发者
1. 重点阅读 [ARCHITECTURE.md](./project-files/docs/ARCHITECTURE.md) 的第 4 章
2. 查看 `frontend/src/` 中的代码结构
3. 查看 `frontend/src/store/` 中的状态管理

---

**文档版本**：v1.0  
**最后更新**：2026-03-04  
**维护者**：Team Up 开发团队
