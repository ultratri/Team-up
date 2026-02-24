package com.teamup.server.modules.project.dto;

import lombok.Data;

/**
 * 项目完成请求DTO
 */
@Data
public class ProjectCompleteRequest {
    
    /**
     * 项目ID
     */
    private Long projectId;
    
    /**
     * 团队处理方式：KEEP-保留团队, DISSOLVE-解散团队
     */
    private String teamAction;
    
    /**
     * 项目总结（可选）
     */
    private String summary;
}
