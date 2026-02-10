package com.teamup.server.modules.ecosystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源标签关联实体类
 */
@Data
@TableName("resource_tags")
public class ResourceTag {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 资源ID
     */
    private Long resourceId;
    
    /**
     * 标签名称
     */
    private String tagName;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
