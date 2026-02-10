package com.teamup.server.modules.ecosystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源实体类
 * 用于资源广场展示项目成果、技术文档、面经分享等
 */
@Data
@TableName("resources")
public class Resource {
    
    /**
     * 资源ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 资源标题
     */
    private String title;
    
    /**
     * 资源描述
     */
    private String description;
    
    /**
     * 资源类型：PROJECT(项目成果)/DOCUMENT(技术文档)/INTERVIEW(面经分享)/MATERIAL(学习资料)
     */
    private String type;
    
    /**
     * 封面图URL
     */
    private String cover;
    
    /**
     * 资源内容（Markdown格式）
     */
    private String content;
    
    /**
     * 作者ID
     */
    private Long authorId;
    
    /**
     * 关联的项目ID（可选）
     */
    private Long projectId;
    
    /**
     * 浏览量
     */
    private Integer views;
    
    /**
     * 点赞数
     */
    private Integer likes;
    
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
     * 逻辑删除标志
     */
    @TableLogic
    private Boolean deleted;
}
