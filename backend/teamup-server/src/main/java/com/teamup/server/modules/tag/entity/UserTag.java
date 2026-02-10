package com.teamup.server.modules.tag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户标签关联实体
 */
@Data
@TableName("user_tags")
public class UserTag {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 标签ID
     */
    private Long tagId;
    
    /**
     * 熟练度：BEGINNER/INTERMEDIATE/ADVANCED/EXPERT
     */
    private String proficiencyLevel;
    
    /**
     * 是否已认证
     */
    private Boolean isVerified;
    
    /**
     * 认证人ID
     */
    private Long verifiedBy;
    
    /**
     * 认证时间
     */
    private LocalDateTime verifiedAt;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
