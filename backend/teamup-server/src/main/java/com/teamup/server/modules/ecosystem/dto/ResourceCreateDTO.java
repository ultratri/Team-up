package com.teamup.server.modules.ecosystem.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建资源DTO
 */
@Data
public class ResourceCreateDTO {
    
    /**
     * 资源标题
     */
    private String title;
    
    /**
     * 资源描述
     */
    private String description;
    
    /**
     * 资源类型
     */
    private String type;
    
    /**
     * 封面图URL
     */
    private String cover;
    
    /**
     * 资源内容
     */
    private String content;
    
    /**
     * 关联的项目ID
     */
    private Long projectId;
    
    /**
     * 标签列表
     */
    private List<String> tags;
}
