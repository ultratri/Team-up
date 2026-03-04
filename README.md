# 🎓 Team Up - 校园组队生态系统

> 基于 **多维技能标签** 与 **双向加权匹配算法** 的一站式校园组队协作平台

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-v1.1-green.svg)]()
[![Status](https://img.shields.io/badge/status-Active-brightgreen.svg)]()

---

## 🌟 项目核心理念

Team Up 不仅是一个组队工具，更是一个**以项目为核心单元**的校园协作生态。我们打破了传统的"先组队后找事"模式，采用**项目驱动 (Project-Driven)** 逻辑：

- **项目是第一公民**：用户围绕具体目标（比赛、科研、实践）创建项目
- **团队是执行实体**：项目招募完成后自动转化为执行团队，支持临时解散或长期复用
- **比赛是可选导向**：项目可关联各类赛事，形成"发现赛事 → 组建项目 → 沉淀成果"的闭环

### 核心价值
✨ **智能匹配** - 基于技能、时间、兴趣的多维加权算法  
🤝 **生态协作** - 从项目发现到团队执行的完整闭环  
🏆 **信誉体系** - 透明的评价和勋章系统激励优质参与  
🆕 **新手友好** - 30天新手光环保护，解决冷启动问题

---

## 🏗️ 核心业务模型

```
用户 → 创建/发现项目 → 智能匹配成员 → 组建团队 → 执行任务 → 沉淀成果 → 信誉评价
```

### 匹配算法权重

**成员找项目** (用户视角)
- 技能匹配: 40%
- 时间可用性: 25%
- 兴趣偏好: 20%
- 比赛偏好: 10%
- 新手加成: 5%

**项目找成员** (项目视角)
- 必需技能: 50%
- 历史信誉: 20%
- 时间投入: 20%
- 新手保护: 10%

---

## 🎨 功能模块详解

### 📱 核心页面导航

#### 1️⃣ **认证模块** (`/auth`)
- **登录页** (`Login.vue`) - 学号/邮箱登录，支持记住密码
- **注册页** (`Register.vue`) - 新用户注册，自动分配新手光环
- **确认页** (`NeedConfirm.vue`) - 邮箱验证确认

#### 2️⃣ **项目模块** (`/project`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **ProjectSquare** | 项目大厅 | 核心招募地，支持筛选、搜索、排序 |
| **ProjectDetail** | 项目详情 | 查看项目信息、成员、技能需求、申请加入 |
| **RecommendedProjects** | 为我推荐 | 基于用户画像的智能推荐项目 |
| **ProjectCandidates** | 推荐候选人 | 项目发起人反向挖掘合适成员 |
| **MyProjects** | 我的项目 | 用户创建/参与的项目管理 |
| **ApplicationManage** | 申请管理 | 管理项目的入队申请 |
| **MatchingResult** | 匹配结果 | 查看匹配分数和推荐理由 |

**核心功能**
- 创建项目：设置技能需求、时间槽、招募人数
- 智能匹配：实时计算匹配度，展示推荐理由
- 申请管理：审核、接受/拒绝入队申请
- 项目转团队：招募完成后自动转化为团队

#### 3️⃣ **团队模块** (`/team`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **TeamList** | 团队列表 | 用户加入的所有团队 |
| **TeamSpace** | 团队工作区 | 团队协作的主入口 |
| **TeamOverview** | 团队概览 | 成员、进度、统计信息 |
| **TaskBoard** | 任务看板 | Kanban 风格的任务管理 |
| **Chat** | 团队聊天 | 实时群聊，支持文件分享 |
| **FileShare** | 文件共享 | 团队文件库，版本管理 |
| **Evaluation** | 成员评估 | 项目完成后的互评系统 |
| **SprintManage** | 冲刺管理 | 敏捷开发的冲刺规划 |
| **DailyStandup** | 日报 | 团队日常进度同步 |
| **TeamSettings** | 团队设置 | 成员管理、权限配置 |
| **InvitationManagement** | 邀请管理 | 管理团队邀请 |
| **TeamCandidates** | 候选成员 | 查看推荐的候选人 |

**核心功能**
- 任务管理：创建、分配、跟踪任务
- 实时协作：Socket.IO 驱动的实时聊天和通知
- 文件管理：上传、分享、版本控制
- 成员评估：项目完成后的互评和信誉计算
- 权限管理：项目经理、成员等多角色权限

#### 4️⃣ **比赛模块** (`/competition`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **CompetitionList** | 比赛广场 | 聚合校内外赛事 |
| **CompetitionDetail** | 比赛详情 | 赛事信息、关联项目、报名 |
| **MentorScoring** | 导师评分 | 导师对参赛项目的评分 |

**核心功能**
- 赛事聚合：展示所有进行中的比赛
- 项目关联：一个项目可关联多个比赛
- 导师评分：支持多维度评分和反馈

#### 5️⃣ **生态广场** (`/ecosystem`)

**EcosystemHub** - 统一的生态入口，包含五大子模块：

1. **项目大厅** - 核心招募地
2. **人才墙** - 展示活跃用户的技能名片
3. **赛事中心** - 聚合校内外赛事
4. **成果广场** - 沉淀项目产出（代码、文档、方案）
5. **任务/悬赏墙** - 短期轻量化协作需求

#### 6️⃣ **导师模块** (`/mentor`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **MentorPlaza** | 导师广场 | 展示所有导师及其指导团队 |
| **MentorApplications** | 指导申请 | 团队申请导师指导 |

**核心功能**
- 导师展示：导师资料、指导领域、评价
- 申请指导：团队可申请导师指导
- 评分反馈：导师对团队的评分和建议

#### 7️⃣ **个人中心** (`/profile`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **ProfileView** | 个人资料 | 用户公开资料展示 |
| **ProfileEdit** | 编辑资料 | 修改个人信息、技能、头像 |
| **ProjectHistory** | 项目历史 | 用户参与过的所有项目 |
| **AccountSettings** | 账户设置 | 密码、隐私、通知设置 |
| **MentorApplication** | 导师申请 | 申请成为导师 |

**核心功能**
- 技能认证：展示已认证的技能
- 信誉分：显示用户的信誉等级和勋章
- 项目历史：完整的参与记录和评价
- 隐私控制：灵活的隐私设置

#### 8️⃣ **消息中心** (`/message`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **MessageCenter** | 消息中心 | 私信、系统消息、通知 |

**核心功能**
- 私信：用户间的一对一沟通
- 系统消息：平台通知和提醒
- 消息分类：按类型组织消息

#### 9️⃣ **通知中心** (`/notification`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **NotificationCenter** | 通知中心 | 实时通知管理 |

**核心功能**
- 实时推送：Socket.IO 驱动的实时通知
- 通知分类：申请、邀请、评价、系统等
- 标记已读：批量管理通知状态

#### 🔟 **数据统计** (`/dashboard`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **StatsDashboard** | 统计仪表板 | 平台数据可视化 |

**核心功能**
- 用户统计：活跃用户、新用户趋势
- 项目统计：项目数、成功率、平均团队规模
- 技能分布：热门技能、技能需求分析
- 比赛统计：参赛项目、获奖情况

#### 1️⃣1️⃣ **管理后台** (`/admin`)

| 页面 | 功能 | 说明 |
|------|------|------|
| **Dashboard** | 管理仪表板 | 平台运营数据 |
| **UserManage** | 用户管理 | 用户审核、禁用、数据导出 |
| **TeamManage** | 团队管理 | 团队审核、解散 |
| **CompetitionManage** | 比赛管理 | 创建、编辑、发布比赛 |
| **MentorManage** | 导师管理 | 导师审核、权限管理 |
| **MentorApplications** | 导师申请审核 | 审核导师申请 |
| **TagManage** | 标签管理 | 技能标签、兴趣标签管理 |
| **AnnouncementManage** | 公告管理 | 发布平台公告 |
| **ContentManage** | 内容管理 | 管理平台内容 |
| **ReportManage** | 举报管理 | 处理用户举报 |
| **AuditLog** | 审计日志 | 查看系统操作日志 |
| **DataExport** | 数据导出 | 导出平台数据 |
| **SystemSettings** | 系统设置 | 平台配置管理 |
| **NewbieProtection** | 新手保护 | 管理新手光环政策 |
| **DepartmentMajorManage** | 院系专业管理 | 管理学校组织结构 |

---

## 🛠️ 技术架构

### 后端服务 (Spring Boot)
```
Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Redis + Socket.IO
```

**核心模块**
- `user` - 用户管理、认证、信誉系统
- `project` - 项目管理、匹配算法集成
- `team` - 团队管理、成员协作
- `matching` - 匹配算法调用、结果缓存
- `competition` - 比赛管理、项目关联
- `mentor` - 导师管理、评分系统
- `evaluation` - 项目评估、互评系统
- `chat` - 实时聊天、Socket.IO 集成
- `notification` - 通知系统、消息推送
- `file` - 文件管理、上传下载
- `report` - 举报管理、内容审核
- `admin` - 后台管理、数据统计

### 匹配服务 (Python FastAPI)
```
Python 3.11 + FastAPI + NumPy + Scikit-learn
```

**职责**
- 用户画像构建：技能、时间、兴趣、历史数据
- 项目需求分析：技能需求、时间要求、团队规模
- 加权匹配计算：多维度权重计算和排序
- 高并发处理：支持实时匹配请求

### 前端应用 (Vue 3)
```
Vue 3 + TypeScript + Vite + Element Plus + Pinia
```

**核心特性**
- 响应式设计：支持桌面和移动设备
- 实时更新：WebSocket 驱动的实时通知
- 状态管理：Pinia 集中式状态管理
- 性能优化：代码分割、懒加载、缓存策略

---

## 🚀 快速启动

### 前置要求
- Node.js 16+
- Java 11+
- Python 3.11+
- MySQL 8.0+
- Redis 6.0+

### 1. 克隆项目
```bash
git clone https://github.com/ultratri/Team-up.git
cd Team-up
```

### 2. 后端服务启动

```bash
cd backend/teamup-server

# 配置数据库连接
# 编辑 src/main/resources/application.yml

# 构建和运行
mvn clean install
mvn spring-boot:run

# 服务运行在 http://localhost:8080
```

### 3. 匹配服务启动

```bash
cd matching-service

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 运行服务
python main.py

# 服务运行在 http://localhost:8000
```

### 4. 前端应用启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 应用运行在 http://localhost:5173
```

### 5. 访问应用
- 前端: http://localhost:5173
- 后端 API: http://localhost:8080
- 匹配服务: http://localhost:8000

---

## 📁 项目结构

```
team-up-project/
├── backend/                           # Spring Boot 后端
│   ├── teamup-server/
│   │   ├── src/main/java/com/teamup/server/
│   │   │   ├── common/                # 通用工具、异常处理
│   │   │   ├── config/                # 配置类
│   │   │   └── modules/               # 业务模块
│   │   │       ├── user/              # 用户管理
│   │   │       ├── project/           # 项目管理
│   │   │       ├── team/              # 团队管理
│   │   │       ├── matching/          # 匹配算法
│   │   │       ├── competition/       # 比赛管理
│   │   │       ├── mentor/            # 导师管理
│   │   │       ├── evaluation/        # 评估系统
│   │   │       ├── chat/              # 聊天功能
│   │   │       ├── notification/      # 通知系统
│   │   │       ├── file/              # 文件管理
│   │   │       ├── report/            # 举报管理
│   │   │       ├── admin/             # 后台管理
│   │   │       └── ...
│   │   └── src/main/resources/
│   │       ├── application.yml        # 应用配置
│   │       └── db/migration/          # 数据库迁移脚本
│   └── pom.xml
│
├── frontend/                          # Vue 3 前端
│   ├── src/
│   │   ├── views/                     # 页面组件
│   │   │   ├── auth/                  # 认证页面
│   │   │   ├── project/               # 项目页面
│   │   │   ├── team/                  # 团队页面
│   │   │   ├── competition/           # 比赛页面
│   │   │   ├── ecosystem/             # 生态广场
│   │   │   ├── mentor/                # 导师页面
│   │   │   ├── profile/               # 个人中心
│   │   │   ├── message/               # 消息中心
│   │   │   ├── notification/          # 通知中心
│   │   │   ├── dashboard/             # 数据统计
│   │   │   └── admin/                 # 管理后台
│   │   ├── components/                # 可复用组件
│   │   ├── api/                       # API 接口
│   │   ├── store/                     # Pinia 状态管理
│   │   ├── router/                    # 路由配置
│   │   ├── types/                     # TypeScript 类型
│   │   ├── utils/                     # 工具函数
│   │   └── styles/                    # 全局样式
│   ├── package.json
│   └── vite.config.ts
│
├── matching-service/                  # Python 匹配服务
│   ├── main.py                        # 服务入口
│   ├── matching-service/
│   │   ├── models/                    # 数据模型
│   │   ├── services/                  # 业务逻辑
│   │   ├── utils/                     # 工具函数
│   │   └── config.py                  # 配置文件
│   └── requirements.txt
│
├── project-files/                     # 非功能文件（不上传）
│   ├── docs/                          # 项目文档
│   ├── backend-docs/                  # 后端文档
│   ├── frontend-docs/                 # 前端文档
│   ├── backend-scripts/               # 后端脚本
│   ├── frontend-scripts/              # 前端脚本
│   ├── config/                        # 配置文件
│   └── ...
│
└── README.md                          # 本文件
```

---

## 🔑 核心特性

### 🎯 智能匹配系统
- 多维度权重算法：技能、时间、兴趣、信誉
- 实时匹配计算：毫秒级响应
- 匹配度可视化：展示推荐理由
- 新手保护机制：30天新手光环

### 🤝 完整协作工具
- 实时聊天：Socket.IO 驱动
- 任务管理：Kanban 风格看板
- 文件共享：版本控制和权限管理
- 日报系统：团队进度同步

### 🏆 信誉激励体系
- 多维度评价：技能、合作、可靠性
- 勋章系统：成就解锁
- 信誉等级：从新手到大师
- 排行榜：激励优质参与

### 📊 数据驱动决策
- 实时统计：用户、项目、技能数据
- 可视化仪表板：趋势分析
- 导出功能：支持数据下载
- 审计日志：完整的操作记录

---

## 🧮 核心算法详解

### 1. 智能匹配算法

#### 成员找项目匹配

**匹配公式**：
```
总匹配度 = 技能匹配(40%) + 时间匹配(25%) + 兴趣匹配(20%) + 比赛匹配(10%) + 新手加成(5%)
```

**技能匹配 (40%)**
```
技能匹配度 = (用户拥有的必需技能数 / 项目所需的必需技能数) × 100%
```

**时间匹配 (25%)**
```
时间匹配度 = min(用户可用时间 / 项目所需时间 × 100%, 100%)
```

**兴趣匹配 (20%)** - 使用余弦相似度
```
相似度 = (A·B) / (|A| × |B|)
兴趣匹配度 = 相似度 × 100%
```

**比赛匹配 (10%)**
```
比赛匹配度 = {
    100%  如果用户对项目关联的比赛感兴趣
    50%   如果用户对该类型比赛感兴趣
    0%    如果用户不感兴趣或项目未关联比赛
}
```

**新手加成 (5%)**
```
新手加成 = {
    5分   如果用户是新手（注册<30天 且 完成项目<3个）
    0分   否则
}
```

#### 项目招募成员匹配

**匹配公式**：
```
总匹配度 = 技能匹配(50%) + 时间匹配(20%) + 信誉评分(20%) + 历史合作(10%)
```

#### 团队找项目匹配

**匹配公式**：
```
总匹配度 = 技能覆盖度(50%) + 规模匹配(20%) + 团队信誉(20%) + 类型匹配(10%)
```

### 2. 信誉评分算法

**信誉分范围**：0-100分  
**初始信誉分**：50分（新用户）  
**新手初始信誉分**：60分

**信誉分更新公式**：
```
新信誉分 = 旧信誉分 × 0.7 + 项目评价分 × 0.3
```

**项目评价分计算**：
```
项目评价分 = (技能水平 + 合作态度 + 可靠性 + 沟通能力) / 4 × 20
```

**信誉等级系统**：

| 信誉分 | 等级 | 徽章 | 说明 |
|--------|------|------|------|
| 0-20 | 新手 | 🌱 | 刚开始参与项目 |
| 21-40 | 初级 | 📚 | 有一定项目经验 |
| 41-60 | 中级 | ⭐ | 项目经验丰富 |
| 61-80 | 高级 | 🏆 | 表现稳定优秀 |
| 81-100 | 大师 | 👑 | 业界认可的高手 |

### 3. 新手保护机制

**新手判定条件**：
- 注册时间 < 30天
- 完成项目 < 3个

**新手加成**：
- 匹配时加成：5分
- 初始信誉加成：10分

**新手任务奖励**：
- 完善个人资料：+10分
- 技能认证：+15分
- 完成首个项目：+20分
- 获得5个好评：+15分
- **总奖励**：60分

---

## 🏗️ 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        用户浏览器                             │
│                    (Vue 3 + TypeScript)                      │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/WebSocket
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                    API 网关 / 负载均衡                        │
│                    (Nginx / HAProxy)                         │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ 后端服务1    │  │ 后端服务2    │  │ 后端服务N    │
│(Spring Boot) │  │(Spring Boot) │  │(Spring Boot) │
└──────────────┘  └──────────────┘  └──────────────┘
        │                │                │
        └────────────────┼────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   MySQL      │  │   Redis      │  │ 匹配服务     │
│  (主数据库)  │  │  (缓存/队列) │  │ (FastAPI)    │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 后端分层架构

```
┌─────────────────────────────────────┐
│      Controller 层 (API 接口)        │
│  - 请求处理                         │
│  - 参数验证                         │
│  - 响应格式化                       │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Service 层 (业务逻辑)           │
│  - 核心业务处理                     │
│  - 事务管理                         │
│  - 调用匹配引擎                     │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Repository 层 (数据访问)        │
│  - 数据库操作                       │
│  - 缓存操作                         │
│  - 查询优化                         │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      数据库 / 缓存                   │
│  - MySQL                            │
│  - Redis                            │
└─────────────────────────────────────┘
```

### 后端模块划分

```
backend/
├── common/
│   ├── api/              # API 响应格式
│   ├── exception/        # 异常处理
│   ├── security/         # 安全认证
│   └── utils/            # 工具类
│
├── config/
│   ├── AsyncConfig       # 异步配置
│   ├── RedisConfig       # Redis 配置
│   └── WebMvcConfig      # Web 配置
│
└── modules/
    ├── user/             # 用户模块
    ├── project/          # 项目模块
    ├── team/             # 团队模块
    ├── matching/         # 匹配模块
    ├── chat/             # 聊天模块
    ├── notification/     # 通知模块
    ├── evaluation/       # 评价模块
    ├── mentor/           # 导师模块
    ├── competition/      # 比赛模块
    ├── file/             # 文件模块
    ├── report/           # 举报模块
    └── admin/            # 管理模块
```

### 前端架构

```
frontend/
├── src/
│   ├── main.ts                  # 应用入口
│   ├── App.vue                  # 根组件
│   │
│   ├── api/                     # API 接口
│   ├── components/              # 可复用组件
│   ├── views/                   # 页面组件
│   ├── store/                   # Pinia 状态管理
│   ├── router/                  # 路由配置
│   ├── types/                   # TypeScript 类型
│   ├── utils/                   # 工具函数
│   ├── styles/                  # 全局样式
│   └── layouts/                 # 布局组件
```

### 请求处理流程

```
HTTP 请求
  ↓
DispatcherServlet (路由匹配)
  ↓
拦截器 (JWT 验证、权限检查)
  ↓
Controller (参数验证、调用 Service)
  ↓
Service (业务逻辑处理、调用 Repository)
  ↓
Repository (数据库/缓存操作)
  ↓
数据库 / 缓存 (返回数据)
  ↓
Service (数据处理、返回结果)
  ↓
Controller (格式化响应、返回 JSON)
  ↓
HTTP 响应
```

---

## 📊 数据库设计

### 核心表结构

#### users (用户表)
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar_url VARCHAR(255),
    bio TEXT,
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    credit_score INT DEFAULT 50,
    completed_projects INT DEFAULT 0,
    is_newbie BOOLEAN DEFAULT TRUE,
    newbie_end_date TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_credit_score (credit_score)
);
```

#### projects (项目表)
```sql
CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    creator_id BIGINT NOT NULL,
    status ENUM('RECRUITING', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED') DEFAULT 'RECRUITING',
    category VARCHAR(50),
    required_skills JSON,
    required_hours_per_week INT,
    start_date DATE,
    end_date DATE,
    max_members INT DEFAULT 5,
    team_id BIGINT,
    competition_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    FOREIGN KEY (competition_id) REFERENCES competitions(id),
    INDEX idx_status (status),
    INDEX idx_creator_id (creator_id),
    INDEX idx_created_at (created_at)
);
```

#### teams (团队表)
```sql
CREATE TABLE teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    leader_id BIGINT NOT NULL,
    type ENUM('TEMPORARY', 'PERMANENT') DEFAULT 'TEMPORARY',
    status ENUM('ACTIVE', 'INACTIVE', 'DISBANDED') DEFAULT 'ACTIVE',
    source_project_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (leader_id) REFERENCES users(id),
    FOREIGN KEY (source_project_id) REFERENCES projects(id),
    INDEX idx_leader_id (leader_id),
    INDEX idx_status (status)
);
```

#### team_members (团队成员表)
```sql
CREATE TABLE team_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('LEADER', 'CORE_MEMBER', 'MEMBER') DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    UNIQUE KEY unique_team_user (team_id, user_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
);
```

#### project_applications (项目申请表)
```sql
CREATE TABLE project_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN') DEFAULT 'PENDING',
    match_score DECIMAL(5, 2),
    match_details JSON,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    UNIQUE KEY unique_project_user (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_project_id (project_id)
);
```

#### evaluations (评价表)
```sql
CREATE TABLE evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    evaluator_id BIGINT NOT NULL,
    evaluated_id BIGINT NOT NULL,
    skill_rating INT,
    cooperation_rating INT,
    reliability_rating INT,
    communication_rating INT,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (evaluator_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (evaluated_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_evaluated_id (evaluated_id),
    INDEX idx_project_id (project_id)
);
```

### 表间关系

**一对多关系 (1:N)**
- users → projects (创建)
- users → teams (领导)
- projects → project_applications (申请)
- teams → team_members (成员)

**多对多关系 (N:N)**
- users ↔ projects (申请)
- users ↔ teams (成员)
- users ↔ users (关注)
- users ↔ projects (收藏)

**自引用关系**
- users → users (评价)

---

## 🤝 贡献指南

### 开发流程
1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 提交规范
遵循 Conventional Commits 规范：
- `feat:` - 新功能
- `fix:` - 修复 Bug
- `docs:` - 文档更新
- `style:` - 代码风格
- `refactor:` - 代码重构
- `perf:` - 性能优化
- `test:` - 测试相关
- `chore:` - 构建、依赖等

---

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

---

## 📞 联系方式

- **问题反馈**: [GitHub Issues](https://github.com/ultratri/Team-up/issues)
- **讨论交流**: [GitHub Discussions](https://github.com/ultratri/Team-up/discussions)

---

## 🙏 致谢

感谢所有贡献者和用户的支持！

---

**版本**: v1.1  
**最后更新**: 2026-03-04  
**维护者**: Team Up 开发团队
