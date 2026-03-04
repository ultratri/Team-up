# 📊 Team Up 可视化图表完整索引

本文档是所有 Mermaid 可视化图表的导航中心。所有图表都使用 Mermaid 语法，可在 GitHub 上直接渲染。

---

## 📑 文档导航

### 1. 🎯 匹配算法流程图
**文件**: `DIAGRAMS_MATCHING_ALGORITHM.md`

包含 10 个详细的匹配算法流程图：

| 图表 | 说明 |
|------|------|
| 成员找项目匹配流程 | 用户浏览项目时的完整匹配过程 |
| 技能匹配度计算 | 如何计算用户与项目的技能匹配度 |
| 时间匹配度计算 | 如何计算用户与项目的时间匹配度 |
| 兴趣匹配度计算 | 使用余弦相似度计算兴趣匹配 |
| 比赛匹配度计算 | 如何计算比赛相关的匹配度 |
| 新手加成计算 | 新手用户的加成计算逻辑 |
| 完整匹配算法流程 | 所有维度的综合匹配流程 |
| 项目招募成员匹配 | 项目创建者查看申请者的匹配过程 |
| 团队找项目匹配 | 长期团队寻找新项目的匹配过程 |
| 匹配算法决策树 | 不同场景下的匹配算法选择 |

**关键公式**:
```
成员找项目: 技能(40%) + 时间(25%) + 兴趣(20%) + 比赛(10%) + 新手(5%)
项目找成员: 技能(50%) + 时间(20%) + 信誉(20%) + 历史(10%)
团队找项目: 覆盖(50%) + 规模(20%) + 信誉(20%) + 类型(10%)
```

---

### 2. 💳 信誉系统流程图
**文件**: `DIAGRAMS_CREDIT_SYSTEM.md`

包含 10 个信誉系统相关的流程图：

| 图表 | 说明 |
|------|------|
| 信誉分更新流程 | 项目完成后信誉分的更新过程 |
| 项目评价分计算 | 如何从 4 个维度计算项目评价分 |
| 信誉等级系统 | 5 个信誉等级及其对应的徽章 |
| 信誉分来源 | 信誉分的三个主要来源 |
| 信誉分影响因素 | 正向和负向因素对信誉分的影响 |
| 新手保护机制 | 新手用户的完整保护流程 |
| 新手判定逻辑 | 如何判断用户是否为新手 |
| 新手任务完成流程 | 新手任务的完成和奖励过程 |
| 信誉分与匹配度关系 | 信誉分如何影响匹配和推荐 |
| 信誉分恢复机制 | 信誉分下降后的恢复机制 |

**关键公式**:
```
新信誉分 = 旧信誉分 × 0.7 + 项目评价分 × 0.3
项目评价分 = (技能 + 合作 + 可靠 + 沟通) / 4 × 20
新手加成 = 5分（注册<30天 且 完成项目<3个）
```

**信誉等级**:
- 🌱 新手 (0-20分)
- 📚 初级 (21-40分)
- ⭐ 中级 (41-60分)
- 🏆 高级 (61-80分)
- 👑 大师 (81-100分)

---

### 3. 📊 数据库 ER 图
**文件**: `DIAGRAMS_DATABASE_ER.md`

包含 10 个数据库相关的图表：

| 图表 | 说明 |
|------|------|
| 核心实体关系图 | 所有表的完整关系图 |
| 用户相关表 | users, user_skills, user_interests 等 |
| 项目相关表 | projects, project_applications, project_favorites |
| 团队相关表 | teams, team_members |
| 评价相关表 | evaluations, mentor_reviews, mentor_member_evaluations |
| 通知和消息表 | notifications, messages |
| 比赛相关表 | competitions |
| 完整数据库关系图 | 所有模块的综合关系图 |
| 表间关系统计 | 一对多、多对多、自引用关系统计 |
| 数据流向图 | 用户操作时的数据流向 |

**核心表**:
- users (用户)
- projects (项目)
- teams (团队)
- project_applications (项目申请)
- team_members (团队成员)
- evaluations (评价)
- competitions (比赛)

---

### 4. 🏗️ 系统架构图
**文件**: `DIAGRAMS_SYSTEM_ARCHITECTURE.md`

包含 10 个系统架构相关的图表：

| 图表 | 说明 |
|------|------|
| 整体系统架构 | 客户端、网关、后端、数据库的完整架构 |
| 后端分层架构 | API 层、业务层、数据访问层、存储层 |
| 后端模块划分 | 8 个业务模块的组织结构 |
| 请求处理流程 | HTTP 请求从入口到响应的完整流程 |
| 前端架构 | Vue 3 应用的目录结构和分层 |
| 状态管理架构 | Pinia Store 的组织和组件通信 |
| WebSocket 实时通信 | 实时消息和通知的架构 |
| Docker Compose 部署 | 容器化部署的架构 |
| Kubernetes 部署 | K8s 集群部署的架构 |
| CI/CD 流程 | 从代码提交到部署的完整流程 |

**核心组件**:
- 前端: Vue 3 + TypeScript + Pinia
- 后端: Spring Boot + MyBatis-Plus
- 匹配服务: FastAPI + Python
- 数据库: MySQL + Redis
- 实时通信: Socket.IO

---

### 5. 🔄 业务流程图
**文件**: `DIAGRAMS_BUSINESS_FLOWS.md`

包含 12 个业务流程相关的图表：

| 图表 | 说明 |
|------|------|
| 用户注册流程 | 从注册到完善资料的完整流程 |
| 项目创建流程 | 从创建到审核通过的流程 |
| 项目申请流程 | 用户申请项目的完整流程 |
| 项目招募完成流程 | 招募完成后的团队创建流程 |
| 项目完成评价流程 | 项目完成后的评价流程 |
| 信誉分更新流程 | 评价后信誉分的更新流程 |
| 新手保护流程 | 新手用户的完整保护流程 |
| 团队邀请流程 | 团队成员邀请的流程 |
| 消息通知流程 | 系统通知的完整流程 |
| 匹配算法执行流程 | 匹配算法的执行过程 |
| 用户搜索和筛选流程 | 用户搜索项目的流程 |
| 项目成果沉淀流程 | 项目成果的发布流程 |

---

## 🎯 按用户角色查看

### 👨‍💻 新开发者
**推荐阅读顺序**:
1. 整体系统架构 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
2. 后端分层架构 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
3. 前端架构 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
4. 核心实体关系图 (DIAGRAMS_DATABASE_ER.md)
5. 请求处理流程 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)

### 🧮 算法工程师
**推荐阅读顺序**:
1. 成员找项目匹配流程 (DIAGRAMS_MATCHING_ALGORITHM.md)
2. 完整匹配算法流程 (DIAGRAMS_MATCHING_ALGORITHM.md)
3. 项目招募成员匹配 (DIAGRAMS_MATCHING_ALGORITHM.md)
4. 团队找项目匹配 (DIAGRAMS_MATCHING_ALGORITHM.md)
5. 信誉分更新流程 (DIAGRAMS_CREDIT_SYSTEM.md)

### 🗄️ 数据库工程师
**推荐阅读顺序**:
1. 核心实体关系图 (DIAGRAMS_DATABASE_ER.md)
2. 完整数据库关系图 (DIAGRAMS_DATABASE_ER.md)
3. 用户相关表 (DIAGRAMS_DATABASE_ER.md)
4. 项目相关表 (DIAGRAMS_DATABASE_ER.md)
5. 数据流向图 (DIAGRAMS_DATABASE_ER.md)

### 🎨 前端开发者
**推荐阅读顺序**:
1. 前端架构 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
2. 状态管理架构 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
3. WebSocket 实时通信 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
4. 用户搜索和筛选流程 (DIAGRAMS_BUSINESS_FLOWS.md)
5. 消息通知流程 (DIAGRAMS_BUSINESS_FLOWS.md)

### 🏗️ 架构师
**推荐阅读顺序**:
1. 整体系统架构 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
2. Docker Compose 部署 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
3. Kubernetes 部署 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
4. CI/CD 流程 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)
5. 后端模块划分 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)

### 📊 产品经理
**推荐阅读顺序**:
1. 用户注册流程 (DIAGRAMS_BUSINESS_FLOWS.md)
2. 项目创建流程 (DIAGRAMS_BUSINESS_FLOWS.md)
3. 项目申请流程 (DIAGRAMS_BUSINESS_FLOWS.md)
4. 成员找项目匹配流程 (DIAGRAMS_MATCHING_ALGORITHM.md)
5. 新手保护流程 (DIAGRAMS_BUSINESS_FLOWS.md)

---

## 🔍 按功能模块查看

### 🔐 认证与授权
- 用户注册流程 (DIAGRAMS_BUSINESS_FLOWS.md)
- 请求处理流程 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)

### 🎯 匹配系统
- 成员找项目匹配流程 (DIAGRAMS_MATCHING_ALGORITHM.md)
- 项目招募成员匹配 (DIAGRAMS_MATCHING_ALGORITHM.md)
- 团队找项目匹配 (DIAGRAMS_MATCHING_ALGORITHM.md)
- 完整匹配算法流程 (DIAGRAMS_MATCHING_ALGORITHM.md)

### 💳 信誉系统
- 信誉分更新流程 (DIAGRAMS_CREDIT_SYSTEM.md)
- 项目评价分计算 (DIAGRAMS_CREDIT_SYSTEM.md)
- 信誉等级系统 (DIAGRAMS_CREDIT_SYSTEM.md)
- 新手保护机制 (DIAGRAMS_CREDIT_SYSTEM.md)

### 📋 项目管理
- 项目创建流程 (DIAGRAMS_BUSINESS_FLOWS.md)
- 项目申请流程 (DIAGRAMS_BUSINESS_FLOWS.md)
- 项目招募完成流程 (DIAGRAMS_BUSINESS_FLOWS.md)
- 项目完成评价流程 (DIAGRAMS_BUSINESS_FLOWS.md)

### 👥 团队管理
- 项目招募完成流程 (DIAGRAMS_BUSINESS_FLOWS.md)
- 团队邀请流程 (DIAGRAMS_BUSINESS_FLOWS.md)

### 💬 通信系统
- 消息通知流程 (DIAGRAMS_BUSINESS_FLOWS.md)
- WebSocket 实时通信 (DIAGRAMS_SYSTEM_ARCHITECTURE.md)

### 🗄️ 数据存储
- 核心实体关系图 (DIAGRAMS_DATABASE_ER.md)
- 完整数据库关系图 (DIAGRAMS_DATABASE_ER.md)
- 数据流向图 (DIAGRAMS_DATABASE_ER.md)

---

## 📈 关键指标和数据

### 匹配算法权重
```
成员找项目:
  - 技能匹配: 40%
  - 时间匹配: 25%
  - 兴趣匹配: 20%
  - 比赛匹配: 10%
  - 新手加成: 5%

项目找成员:
  - 技能匹配: 50%
  - 时间匹配: 20%
  - 信誉评分: 20%
  - 历史合作: 10%

团队找项目:
  - 技能覆盖度: 50%
  - 规模匹配: 20%
  - 团队信誉: 20%
  - 类型匹配: 10%
```

### 信誉系统
```
初始信誉分: 50分
新手初始信誉分: 60分
信誉分范围: 0-100分

信誉等级:
  - 新手: 0-20分 (🌱)
  - 初级: 21-40分 (📚)
  - 中级: 41-60分 (⭐)
  - 高级: 61-80分 (🏆)
  - 大师: 81-100分 (👑)

信誉分更新公式:
  新信誉分 = 旧信誉分 × 0.7 + 项目评价分 × 0.3

项目评价分计算:
  项目评价分 = (技能 + 合作 + 可靠 + 沟通) / 4 × 20
```

### 新手保护
```
新手判定条件:
  - 注册时间 < 30天
  - 完成项目 < 3个

新手加成:
  - 匹配时加成: 5分
  - 初始信誉加成: 10分

新手任务奖励:
  - 完善个人资料: +10分
  - 技能认证: +15分
  - 完成首个项目: +20分
  - 获得5个好评: +15分
  - 总奖励: 60分
```

---

## 🔗 相关文档

- `ALGORITHMS.md` - 详细算法说明（文本版本）
- `ARCHITECTURE.md` - 详细架构说明（文本版本）
- `DATABASE_ER_DIAGRAM.md` - 详细数据库设计（文本版本）
- `ALGORITHMS_AND_ARCHITECTURE.md` - 快速参考索引
- `README.md` - 项目总体介绍

---

## 💡 使用建议

1. **首次了解项目**: 从"整体系统架构"开始，获得全局视图
2. **深入学习模块**: 根据角色选择相关的流程图
3. **参考实现**: 结合文本文档和图表，理解具体实现
4. **团队讨论**: 使用这些图表进行技术讨论和知识分享
5. **文档维护**: 当系统架构或流程变化时，及时更新相应的图表

---

**文档版本**: v1.0  
**最后更新**: 2026-03-04  
**维护者**: Team Up 开发团队

