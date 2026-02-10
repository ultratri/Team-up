package com.teamup.server.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_settings")
public class SystemSetting {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置键（唯一）
     */
    private String settingKey;
    
    /**
     * 配置值（JSON 格式）
     */
    private String settingValue;
    
    /**
     * 配置分组（basic, notification, security 等）
     */
    private String settingGroup;
    
    /**
     * 配置描述
     */
    private String description;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
