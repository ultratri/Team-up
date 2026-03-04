# 🎯 Team Up 匹配算法流程图

## 1. 成员找项目匹配流程

```mermaid
graph TD
    A["用户浏览项目大厅"] --> B["获取用户信息"]
    B --> C["获取项目信息"]
    C --> D["计算技能匹配度 40%"]
    D --> E["计算时间匹配度 25%"]
    E --> F["计算兴趣匹配度 20%"]
    F --> G["计算比赛匹配度 10%"]
    G --> H["计算新手加成 5%"]
    H --> I["加权求和"]
    I --> J["总匹配度 = 0-100"]
    J --> K["按匹配度排序展示"]
    K --> L["用户查看推荐项目"]
    
    style A fill:#e1f5ff
    style J fill:#c8e6c9
    style L fill:#fff9c4
```

## 2. 技能匹配度计算

```mermaid
graph LR
    A["用户技能集合"] --> B["提取必需技能"]
    C["项目必需技能"] --> B
    B --> D["求交集"]
    D --> E["计算比例"]
    E --> F["技能匹配度 = 匹配数/需求数 × 100%"]
    
    style F fill:#c8e6c9
```

## 3. 时间匹配度计算

```mermaid
graph LR
    A["用户可用时间"] --> B["比较"]
    C["项目需求时间"] --> B
    B --> D["计算比例"]
    D --> E["取上限100%"]
    E --> F["时间匹配度 = min用户时间/项目时间 × 100%, 100%"]
    
    style F fill:#c8e6c9
```

## 4. 兴趣匹配度计算 - 余弦相似度

```mermaid
graph TD
    A["用户兴趣向量"] --> B["构建向量空间"]
    C["项目类型向量"] --> B
    B --> D["计算点积"]
    D --> E["计算向量模"]
    E --> F["计算余弦相似度"]
    F --> G["相似度 = 点积 / 模1 × 模2"]
    G --> H["兴趣匹配度 = 相似度 × 100%"]
    
    style H fill:#c8e6c9
```

## 5. 比赛匹配度计算

```mermaid
graph TD
    A["项目是否关联比赛?"] -->|否| B["比赛匹配度 = 0%"]
    A -->|是| C["用户是否对该比赛感兴趣?"]
    C -->|是| D["比赛匹配度 = 100%"]
    C -->|否| E["用户是否对该类型比赛感兴趣?"]
    E -->|是| F["比赛匹配度 = 50%"]
    E -->|否| G["比赛匹配度 = 0%"]
    
    style B fill:#ffccbc
    style D fill:#c8e6c9
    style F fill:#fff9c4
    style G fill:#ffccbc
```

## 6. 新手加成计算

```mermaid
graph TD
    A["用户注册时间 < 30天?"] -->|否| B["新手加成 = 0分"]
    A -->|是| C["用户完成项目 < 3个?"]
    C -->|否| D["新手加成 = 0分"]
    C -->|是| E["新手加成 = 5分"]
    
    style B fill:#ffccbc
    style D fill:#ffccbc
    style E fill:#c8e6c9
```

## 7. 完整匹配算法流程

```mermaid
graph TD
    A["开始"] --> B["获取用户信息"]
    B --> C["获取项目信息"]
    C --> D["计算技能匹配 40%"]
    D --> E["计算时间匹配 25%"]
    E --> F["计算兴趣匹配 20%"]
    F --> G["计算比赛匹配 10%"]
    G --> H["计算新手加成 5%"]
    H --> I["加权求和"]
    I --> J["总匹配度 = skill×0.4 + time×0.25 + interest×0.2 + competition×0.1 + newbie×0.05"]
    J --> K["取上限100%"]
    K --> L["返回匹配度"]
    L --> M["结束"]
    
    style A fill:#e1f5ff
    style M fill:#ffccbc
    style J fill:#c8e6c9
```

## 8. 项目招募成员匹配流程

```mermaid
graph TD
    A["项目创建者查看申请者"] --> B["获取申请者信息"]
    B --> C["计算技能匹配 50%"]
    C --> D["计算时间匹配 20%"]
    D --> E["计算信誉评分 20%"]
    E --> F["计算历史合作 10%"]
    F --> G["加权求和"]
    G --> H["总匹配度 = skill×0.5 + time×0.2 + credit×0.2 + history×0.1"]
    H --> I["按匹配度排序"]
    I --> J["展示推荐申请者"]
    
    style A fill:#e1f5ff
    style H fill:#c8e6c9
    style J fill:#fff9c4
```

## 9. 团队找项目匹配流程

```mermaid
graph TD
    A["长期团队寻找新项目"] --> B["获取团队成员信息"]
    B --> C["计算技能覆盖度 50%"]
    C --> D["计算规模匹配 20%"]
    D --> E["计算团队信誉 20%"]
    E --> F["计算类型匹配 10%"]
    F --> G["加权求和"]
    G --> H["总匹配度 = coverage×0.5 + scale×0.2 + credit×0.2 + type×0.1"]
    H --> I["按匹配度排序"]
    I --> J["展示推荐项目"]
    
    style A fill:#e1f5ff
    style H fill:#c8e6c9
    style J fill:#fff9c4
```

## 10. 匹配算法决策树

```mermaid
graph TD
    A["匹配场景"] --> B{场景类型}
    B -->|成员找项目| C["权重: 技能40% 时间25% 兴趣20% 比赛10% 新手5%"]
    B -->|项目找成员| D["权重: 技能50% 时间20% 信誉20% 历史10%"]
    B -->|团队找项目| E["权重: 覆盖50% 规模20% 信誉20% 类型10%"]
    
    C --> F["返回0-100分"]
    D --> F
    E --> F
    F --> G["按分数排序"]
    G --> H["展示结果"]
    
    style A fill:#e1f5ff
    style F fill:#c8e6c9
    style H fill:#fff9c4
```

---

**文档版本**：v1.0  
**最后更新**：2026-03-04
