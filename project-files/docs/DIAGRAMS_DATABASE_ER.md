# 📊 Team Up 数据库 ER 图 - Mermaid 版本

## 1. 核心实体关系图

```mermaid
erDiagram
    USERS ||--o{ PROJECTS : creates
    USERS ||--o{ TEAMS : leads
    USERS ||--o{ PROJECT_APPLICATIONS : submits
    USERS ||--o{ TEAM_MEMBERS : joins
    USERS ||--o{ EVALUATIONS : gives
    USERS ||--o{ EVALUATIONS : receives
    USERS ||--o{ USER_SKILLS : has
    USERS ||--o{ USER_INTERESTS : has
    USERS ||--o{ USER_AVAILABILITY : has
    USERS ||--o{ USER_FOLLOWS : follows
    USERS ||--o{ USER_FOLLOWS : followed_by
    USERS ||--o{ PROJECT_FAVORITES : favorites
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ MESSAGES : sends
    USERS ||--o{ MESSAGES : receives
    
    PROJECTS ||--o{ PROJECT_APPLICATIONS : receives
    PROJECTS ||--o{ TEAM_MEMBERS : has
    PROJECTS ||--o{ EVALUATIONS : evaluated_in
    PROJECTS ||--o{ PROJECT_FAVORITES : favorited_by
    PROJECTS ||--o{ COMPETITIONS : associated_with
    PROJECTS ||--|| TEAMS : transforms_to
    
    TEAMS ||--o{ TEAM_MEMBERS : contains
    TEAMS ||--o{ PROJECTS : executes
    
    COMPETITIONS ||--o{ PROJECTS : has
```

## 2. 用户相关表

```mermaid
erDiagram
    USERS {
        bigint id PK
        string username UK
        string email UK
        string password_hash
        string nickname
        string avatar_url
        text bio
        enum status
        timestamp registration_date
        int credit_score
        int completed_projects
        boolean is_newbie
        timestamp newbie_end_date
    }
    
    USER_SKILLS {
        bigint id PK
        bigint user_id FK
        string skill_name
        enum proficiency_level
        boolean is_certified
    }
    
    USER_INTERESTS {
        bigint id PK
        bigint user_id FK
        string interest_name
        int interest_level
    }
    
    USER_AVAILABILITY {
        bigint id PK
        bigint user_id FK
        int available_hours
        json time_slots
    }
    
    USER_FOLLOWS {
        bigint id PK
        bigint follower_id FK
        bigint following_id FK
        timestamp created_at
    }
    
    USERS ||--o{ USER_SKILLS : has
    USERS ||--o{ USER_INTERESTS : has
    USERS ||--o{ USER_AVAILABILITY : has
    USERS ||--o{ USER_FOLLOWS : follows
```

## 3. 项目相关表

```mermaid
erDiagram
    PROJECTS {
        bigint id PK
        string name
        text description
        bigint creator_id FK
        enum status
        string category
        json required_skills
        int required_hours_per_week
        date start_date
        date end_date
        int max_members
        bigint team_id FK
        bigint competition_id FK
        timestamp created_at
        timestamp updated_at
    }
    
    PROJECT_APPLICATIONS {
        bigint id PK
        bigint project_id FK
        bigint user_id FK
        enum status
        decimal match_score
        json match_details
        timestamp applied_at
        timestamp reviewed_at
    }
    
    PROJECT_FAVORITES {
        bigint id PK
        bigint user_id FK
        bigint project_id FK
        timestamp created_at
    }
    
    PROJECTS ||--o{ PROJECT_APPLICATIONS : receives
    PROJECTS ||--o{ PROJECT_FAVORITES : favorited_by
```

## 4. 团队相关表

```mermaid
erDiagram
    TEAMS {
        bigint id PK
        string name
        text description
        bigint leader_id FK
        enum type
        enum status
        bigint source_project_id FK
        timestamp created_at
        timestamp updated_at
    }
    
    TEAM_MEMBERS {
        bigint id PK
        bigint team_id FK
        bigint user_id FK
        enum role
        timestamp joined_at
        timestamp left_at
    }
    
    TEAMS ||--o{ TEAM_MEMBERS : contains
```

## 5. 评价相关表

```mermaid
erDiagram
    EVALUATIONS {
        bigint id PK
        bigint project_id FK
        bigint evaluator_id FK
        bigint evaluated_id FK
        int skill_rating
        int cooperation_rating
        int reliability_rating
        int communication_rating
        text comment
        timestamp created_at
    }
    
    MENTOR_REVIEWS {
        bigint id PK
        bigint mentor_id FK
        bigint project_id FK
        int rating
        text comment
        timestamp created_at
    }
    
    MENTOR_MEMBER_EVALUATIONS {
        bigint id PK
        bigint mentor_id FK
        bigint member_id FK
        bigint project_id FK
        int score
        text feedback
        timestamp created_at
    }
```

## 6. 通知和消息表

```mermaid
erDiagram
    NOTIFICATIONS {
        bigint id PK
        bigint recipient_id FK
        enum type
        text content
        boolean is_read
        timestamp created_at
    }
    
    MESSAGES {
        bigint id PK
        bigint sender_id FK
        bigint receiver_id FK
        text content
        boolean is_read
        timestamp created_at
    }
    
    NOTIFICATIONS ||--o{ USERS : sent_to
    MESSAGES ||--o{ USERS : sent_by
    MESSAGES ||--o{ USERS : sent_to
```

## 7. 比赛相关表

```mermaid
erDiagram
    COMPETITIONS {
        bigint id PK
        string name
        text description
        date start_date
        date end_date
        enum status
        string category
        timestamp created_at
    }
    
    COMPETITIONS ||--o{ PROJECTS : has
```

## 8. 完整数据库关系图

```mermaid
graph TB
    subgraph Users["用户模块"]
        U["users"]
        US["user_skills"]
        UI["user_interests"]
        UA["user_availability"]
        UF["user_follows"]
    end
    
    subgraph Projects["项目模块"]
        P["projects"]
        PA["project_applications"]
        PF["project_favorites"]
    end
    
    subgraph Teams["团队模块"]
        T["teams"]
        TM["team_members"]
    end
    
    subgraph Evaluation["评价模块"]
        E["evaluations"]
        MR["mentor_reviews"]
        MME["mentor_member_evaluations"]
    end
    
    subgraph Communication["通信模块"]
        N["notifications"]
        M["messages"]
    end
    
    subgraph Competition["比赛模块"]
        C["competitions"]
    end
    
    U --> US
    U --> UI
    U --> UA
    U --> UF
    U --> P
    U --> T
    U --> PA
    U --> PF
    U --> E
    U --> N
    U --> M
    
    P --> PA
    P --> PF
    P --> E
    P --> T
    P --> C
    
    T --> TM
    TM --> U
    
    E --> U
    MR --> U
    MME --> U
    
    N --> U
    M --> U
    
    C --> P
    
    style Users fill:#e1f5ff
    style Projects fill:#f3e5f5
    style Teams fill:#e8f5e9
    style Evaluation fill:#fff3e0
    style Communication fill:#fce4ec
    style Competition fill:#f1f8e9
```

## 9. 表间关系统计

```mermaid
graph LR
    A["一对多关系"] --> B["users → projects"]
    A --> C["users → teams"]
    A --> D["projects → project_applications"]
    A --> E["teams → team_members"]
    
    F["多对多关系"] --> G["users ↔ projects"]
    F --> H["users ↔ teams"]
    F --> I["users ↔ users"]
    
    J["自引用关系"] --> K["users → users"]
    
    style A fill:#c8e6c9
    style F fill:#fff9c4
    style J fill:#ffccbc
```

## 10. 数据流向图

```mermaid
graph TD
    A["用户注册"] --> B["创建 users 记录"]
    B --> C["初始化 user_skills"]
    C --> D["初始化 user_interests"]
    D --> E["初始化 user_availability"]
    
    F["用户创建项目"] --> G["创建 projects 记录"]
    G --> H["项目进入招募状态"]
    
    I["用户申请项目"] --> J["创建 project_applications 记录"]
    J --> K["计算匹配度"]
    K --> L["更新 match_score"]
    
    M["项目招募完成"] --> N["创建 teams 记录"]
    N --> O["创建 team_members 记录"]
    
    P["项目完成"] --> Q["创建 evaluations 记录"]
    Q --> R["更新 users.credit_score"]
    
    style B fill:#c8e6c9
    style G fill:#c8e6c9
    style J fill:#c8e6c9
    style N fill:#c8e6c9
    style Q fill:#c8e6c9
```

---

**文档版本**：v1.0  
**最后更新**：2026-03-04
