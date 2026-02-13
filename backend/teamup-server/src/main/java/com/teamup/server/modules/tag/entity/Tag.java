package com.teamup.server.modules.tag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签实体
 */
@Data
@TableName("tags")
public class Tag {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 标签名称
     */
    private String name;
    
    /**
     * 标签分类（存储为字符串：SKILL, INTEREST, PERSONALITY, PROJECT_TYPE）
     */
    private String category;
    
    /**
     * 父标签ID
     */
    private Long parentId;
    
    /**
     * 标签描述
     */
    private String description;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
    /**
     * 是否官方标签
     */
    private Boolean isOfficial;
    
    /**
     * 状态（存储为字符串：ACTIVE, DEPRECATED, MERGED）
     */
    private String status;
    
    /**
     * 合并到的标签ID
     */
    private Long mergedToId;
    
    /**
     * 创建者ID
     */
    private Long createdBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 标签分类枚举
     */
    public enum TagCategory {
        SKILL,          // 技能标签
        INTEREST,       // 兴趣标签
        PERSONALITY,    // 性格标签
        PROJECT_TYPE    // 项目类型标签
    }
    
    /**
     * 标签状态枚举
     */
    public enum TagStatus {
        ACTIVE,      // 活跃
        DEPRECATED,  // 已废弃
        MERGED       // 已合并
    }
}
