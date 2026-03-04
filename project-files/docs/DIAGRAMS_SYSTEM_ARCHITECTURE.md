# 🏗️ Team Up 系统架构图 - Mermaid 版本

## 1. 整体系统架构

```mermaid
graph TB
    subgraph Client["客户端层"]
        WEB["Web 浏览器<br/>Vue 3 + TypeScript"]
        MOBILE["移动应用<br/>可选"]
    end
    
    subgraph Gateway["网关层"]
        LB["负载均衡<br/>Nginx/HAProxy"]
        CORS["CORS 处理"]
    end
    
    subgraph Backend["后端服务层"]
        BS1["后端服务1<br/>Spring Boot"]
        BS2["后端服务2<br/>Spring Boot"]
        BSN["后端服务N<br/>Spring Boot"]
    end
    
    subgraph Matching["匹配引擎"]
        MS["匹配服务<br/>FastAPI"]
    end
    
    subgraph Data["数据层"]
        DB["MySQL<br/>主数据库"]
        CACHE["Redis<br/>缓存/队列"]
    end
    
    subgraph RealTime["实时通信"]
        WS["WebSocket<br/>Socket.IO"]
    end
    
    Client --> Gateway
    Gateway --> Backend
    Backend --> Matching
    Backend --> Data
    Backend --> RealTime
    Matching --> Data
    
    style Client fill:#e1f5ff
    style Gateway fill:#f3e5f5
    style Backend fill:#e8f5e9
    style Matching fill:#fff3e0
    style Data fill:#fce4ec
    style RealTime fill:#f1f8e9
```

## 2. 后端分层架构

```mermaid
graph TD
    subgraph API["API 层"]
        CTRL["Controller<br/>- 请求处理<br/>- 参数验证<br/>- 响应格式化"]
    end
    
    subgraph Business["业务逻辑层"]
        SVC["Service<br/>- 核心业务处理<br/>- 事务管理<br/>- 调用匹配引擎"]
    end
    
    subgraph Data["数据访问层"]
        REPO["Repository<br/>- 数据库操作<br/>- 缓存操作<br/>- 查询优化"]
    end
    
    subgraph Storage["存储层"]
        DB["MySQL<br/>主存储"]
        REDIS["Redis<br/>缓存"]
    end
    
    CTRL --> SVC
    SVC --> REPO
    REPO --> DB
    REPO --> REDIS
    
    style API fill:#e1f5ff
    style Business fill:#f3e5f5
    style Data fill:#e8f5e9
    style Storage fill:#fff3e0
```

## 3. 后端模块划分

```mermaid
graph TB
    subgraph Common["通用模块"]
        API["API 响应格式"]
        EXC["异常处理"]
        SEC["安全认证"]
        UTIL["工具类"]
    end
    
    subgraph Config["配置模块"]
        ASYNC["异步配置"]
        REDIS["Redis 配置"]
        WEB["Web 配置"]
    end
    
    subgraph Business["业务模块"]
        USER["用户模块<br/>- 注册/登录<br/>- 资料管理<br/>- 信誉系统"]
        PROJECT["项目模块<br/>- 创建/招募<br/>- 匹配<br/>- 管理"]
        TEAM["团队模块<br/>- 创建/管理<br/>- 成员协作<br/>- 解散"]
        MATCHING["匹配模块<br/>- 调用 FastAPI<br/>- 缓存结果<br/>- 异步处理"]
        CHAT["聊天模块<br/>- 群聊<br/>- 私聊<br/>- 消息存储"]
        NOTIF["通知模块<br/>- 系统通知<br/>- 实时推送<br/>- 消息队列"]
        EVAL["评价模块<br/>- 项目评价<br/>- 成员互评<br/>- 信誉更新"]
        MENTOR["导师模块<br/>- 导师评分<br/>- 成员评价<br/>- 反馈管理"]
    end
    
    Common --> Business
    Config --> Business
    
    style Common fill:#e1f5ff
    style Config fill:#f3e5f5
    style Business fill:#e8f5e9
```

## 4. 请求处理流程

```mermaid
graph TD
    A["HTTP 请求"] --> B["DispatcherServlet<br/>路由匹配"]
    B --> C["拦截器<br/>JWT 验证<br/>权限检查"]
    C --> D{权限检查}
    D -->|失败| E["返回 401/403"]
    D -->|成功| F["Controller<br/>参数验证"]
    F --> G{参数有效?}
    G -->|否| H["返回 400"]
    G -->|是| I["Service<br/>业务逻辑处理"]
    I --> J["调用 Repository"]
    J --> K["数据库/缓存操作"]
    K --> L["返回数据"]
    L --> M["Service 处理结果"]
    M --> N["Controller 格式化"]
    N --> O["返回 JSON 响应"]
    
    style A fill:#e1f5ff
    style O fill:#c8e6c9
    style E fill:#ffccbc
    style H fill:#ffccbc
```

## 5. 前端架构

```mermaid
graph TB
    subgraph Entry["入口"]
        MAIN["main.ts<br/>应用入口"]
        APP["App.vue<br/>根组件"]
    end
    
    subgraph Router["路由层"]
        ROUTE["Router<br/>- 路由配置<br/>- 导航守卫<br/>- 权限控制"]
    end
    
    subgraph State["状态管理"]
        PINIA["Pinia Store<br/>- auth<br/>- user<br/>- project<br/>- team"]
    end
    
    subgraph View["视图层"]
        VIEWS["Views<br/>- 页面组件<br/>- 业务逻辑"]
        COMP["Components<br/>- 可复用组件<br/>- UI 组件"]
    end
    
    subgraph API["API 层"]
        APICLIENT["API Client<br/>- HTTP 请求<br/>- WebSocket<br/>- 错误处理"]
    end
    
    subgraph Utils["工具层"]
        UTIL["Utils<br/>- 工具函数<br/>- 格式化<br/>- 验证"]
        TYPES["Types<br/>- TypeScript 类型<br/>- 接口定义"]
    end
    
    MAIN --> APP
    APP --> ROUTE
    ROUTE --> VIEWS
    VIEWS --> COMP
    VIEWS --> PINIA
    COMP --> PINIA
    PINIA --> APICLIENT
    APICLIENT --> UTIL
    APICLIENT --> TYPES
    
    style Entry fill:#e1f5ff
    style Router fill:#f3e5f5
    style State fill:#e8f5e9
    style View fill:#fff3e0
    style API fill:#fce4ec
    style Utils fill:#f1f8e9
```

## 6. 状态管理架构

```mermaid
graph TD
    subgraph Store["Pinia Store"]
        AUTH["auth store<br/>- token<br/>- isLogin<br/>- user"]
        USER["user store<br/>- userInfo<br/>- credits<br/>- skills"]
        PROJECT["project store<br/>- projects<br/>- filters<br/>- current"]
        TEAM["team store<br/>- teams<br/>- members<br/>- current"]
    end
    
    subgraph Components["Vue 组件"]
        C1["组件1"]
        C2["组件2"]
        C3["组件3"]
    end
    
    Store --> C1
    Store --> C2
    Store --> C3
    
    C1 -.->|dispatch| Store
    C2 -.->|dispatch| Store
    C3 -.->|dispatch| Store
    
    style Store fill:#c8e6c9
    style Components fill:#fff9c4
```

## 7. WebSocket 实时通信架构

```mermaid
graph TD
    subgraph Client["前端"]
        WS_CLIENT["WebSocket 客户端<br/>Socket.IO"]
    end
    
    subgraph Server["后端"]
        WS_SERVER["WebSocket 服务器<br/>Socket.IO"]
        HANDLER["事件处理器"]
        SERVICE["业务服务"]
    end
    
    subgraph Events["事件类型"]
        MSG["消息事件<br/>- message:send<br/>- message:receive"]
        NOTIF["通知事件<br/>- notification:new<br/>- notification:read"]
        TASK["任务事件<br/>- task:update<br/>- task:complete"]
        USER["用户事件<br/>- user:online<br/>- user:offline"]
    end
    
    subgraph Storage["存储"]
        DB["数据库"]
        CACHE["缓存"]
    end
    
    WS_CLIENT -->|连接| WS_SERVER
    WS_CLIENT -->|发送事件| HANDLER
    HANDLER -->|处理| SERVICE
    SERVICE -->|存储| DB
    SERVICE -->|缓存| CACHE
    SERVICE -->|广播| WS_SERVER
    WS_SERVER -->|推送| WS_CLIENT
    
    HANDLER --> MSG
    HANDLER --> NOTIF
    HANDLER --> TASK
    HANDLER --> USER
    
    style Client fill:#e1f5ff
    style Server fill:#f3e5f5
    style Events fill:#e8f5e9
    style Storage fill:#fff3e0
```

## 8. 部署架构 - Docker Compose

```mermaid
graph TB
    subgraph Docker["Docker Compose"]
        subgraph Frontend["前端容器"]
            FE["Frontend<br/>Nginx + Vue"]
        end
        
        subgraph Backend["后端容器"]
            BE["Backend<br/>Spring Boot"]
        end
        
        subgraph Matching["匹配服务容器"]
            MS["Matching Service<br/>FastAPI"]
        end
        
        subgraph Database["数据库容器"]
            DB["MySQL<br/>数据库"]
            REDIS["Redis<br/>缓存"]
        end
    end
    
    FE --> BE
    BE --> MS
    BE --> DB
    BE --> REDIS
    MS --> DB
    
    style Docker fill:#e1f5ff
    style Frontend fill:#f3e5f5
    style Backend fill:#e8f5e9
    style Matching fill:#fff3e0
    style Database fill:#fce4ec
```

## 9. Kubernetes 部署架构

```mermaid
graph TB
    subgraph K8s["Kubernetes Cluster"]
        subgraph Ingress["入口层"]
            ING["Ingress<br/>Nginx<br/>- 路由<br/>- SSL/TLS"]
        end
        
        subgraph Service["服务层"]
            SVC1["Service<br/>负载均衡"]
            SVC2["Service<br/>负载均衡"]
        end
        
        subgraph Deployment["部署层"]
            DEP1["Deployment<br/>Backend Replicas"]
            DEP2["Deployment<br/>Frontend Replicas"]
            DEP3["Deployment<br/>Matching Replicas"]
        end
        
        subgraph StatefulSet["有状态集合"]
            SS1["StatefulSet<br/>MySQL"]
            SS2["StatefulSet<br/>Redis"]
        end
    end
    
    ING --> SVC1
    ING --> SVC2
    SVC1 --> DEP1
    SVC1 --> DEP3
    SVC2 --> DEP2
    DEP1 --> SS1
    DEP1 --> SS2
    DEP3 --> SS1
    
    style K8s fill:#e1f5ff
    style Ingress fill:#f3e5f5
    style Service fill:#e8f5e9
    style Deployment fill:#fff3e0
    style StatefulSet fill:#fce4ec
```

## 10. CI/CD 流程

```mermaid
graph LR
    A["Git Push"] --> B["GitHub Actions"]
    B --> C["代码检查<br/>Lint"]
    C --> D["单元测试"]
    D --> E["集成测试"]
    E --> F{测试通过?}
    F -->|否| G["构建失败<br/>通知开发者"]
    F -->|是| H["Docker Build<br/>构建镜像"]
    H --> I["推送到仓库<br/>Docker Registry"]
    I --> J["Kubernetes Deploy<br/>更新 Deployment"]
    J --> K["滚动更新<br/>Rolling Update"]
    K --> L["健康检查<br/>Health Check"]
    L --> M{检查通过?}
    M -->|否| N["回滚<br/>Rollback"]
    M -->|是| O["部署完成"]
    
    style A fill:#e1f5ff
    style O fill:#c8e6c9
    style G fill:#ffccbc
    style N fill:#ffccbc
```

---

**文档版本**：v1.0  
**最后更新**：2026-03-04
