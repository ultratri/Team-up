# 校园组队平台 - 前端应用

基于 Vue 3 + TypeScript + Vite 构建的现代化前端应用。

## 技术栈

- **框架**: Vue 3.5+ (Composition API)
- **构建工具**: Vite 7+
- **语言**: TypeScript 5+
- **UI框架**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **WebSocket**: Socket.io-client
- **图表库**: ECharts + D3.js
- **动画库**: GSAP
- **工具库**: VueUse
- **代码规范**: ESLint + Prettier

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API接口定义
│   ├── assets/           # 静态资源
│   │   ├── images/       # 图片
│   │   └── styles/       # 样式文件
│   ├── components/       # 组件
│   │   ├── common/       # 通用组件
│   │   ├── charts/       # 图表组件
│   │   ├── animations/   # 动画组件
│   │   ├── profile/      # 个人中心组件
│   │   └── team/         # 团队相关组件
│   ├── views/            # 页面组件
│   │   ├── auth/         # 认证页面
│   │   ├── profile/      # 个人中心
│   │   ├── project/      # 项目广场
│   │   ├── team/         # 团队协作
│   │   └── admin/        # 管理后台
│   ├── store/            # Pinia状态管理
│   ├── router/           # 路由配置
│   ├── utils/            # 工具函数
│   ├── composables/      # 组合式函数
│   ├── types/            # TypeScript类型定义
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── public/               # 公共静态资源
├── .eslintrc.cjs         # ESLint配置
├── .prettierrc.json      # Prettier配置
├── vite.config.ts        # Vite配置
├── tsconfig.json         # TypeScript配置
└── package.json          # 项目依赖
```

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

应用将在 `http://localhost:3000` 启动。

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

### 代码检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

### 类型检查

```bash
npm run type-check
```

## 路径别名

项目配置了以下路径别名：

- `@/` → `src/`
- `@api/` → `src/api/`
- `@components/` → `src/components/`
- `@views/` → `src/views/`
- `@store/` → `src/store/`
- `@router/` → `src/router/`
- `@utils/` → `src/utils/`
- `@composables/` → `src/composables/`
- `@types/` → `src/types/`
- `@assets/` → `src/assets/`
- `@styles/` → `src/assets/styles/`

## 核心功能模块

### 1. 个人成长中心
- 用户档案管理
- 技能标签系统
- 能力雷达图
- 时间可用性设置
- 项目履历墙
- 信誉积分系统

### 2. 项目孵化广场
- 项目浏览与搜索
- 智能匹配推荐
- 项目创建与发布
- 申请与审核

### 3. 团队协作空间
- 任务看板（Kanban）
- 文件共享
- 实时聊天
- 成员管理
- 团队互评

### 4. 管理后台
- 用户管理
- 项目审核
- 标签库管理
- 数据统计看板

## 开发规范

### 组件命名

- 使用 PascalCase 命名组件文件
- 组件名应该是多个单词组成（避免与HTML元素冲突）

### 代码风格

- 使用 Composition API
- 使用 `<script setup>` 语法糖
- 使用 TypeScript 进行类型标注
- 遵循 ESLint 和 Prettier 规则

### 提交规范

遵循 Conventional Commits 规范：

- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具配置

## 环境变量

创建 `.env.local` 文件配置本地环境变量：

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_SOCKET_URL=http://localhost:4000
```

## 浏览器兼容性

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## License

MIT
