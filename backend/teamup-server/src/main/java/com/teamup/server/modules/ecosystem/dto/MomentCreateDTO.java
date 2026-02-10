package com.teamup.server.modules.ecosystem.dto;

import lombok.Data;

/**
 * 创建动态DTO
 */
@Data
public class MomentCreateDTO {
    
    /**
     * 动态类型
     */
    private String type;
    
    /**
     * 动态内容
     */
    private String content;
    
    /**
     * 相关项目ID
     */
    private Long relatedProjectId;
}
