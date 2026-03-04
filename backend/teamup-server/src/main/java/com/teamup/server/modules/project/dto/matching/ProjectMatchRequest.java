package com.teamup.server.modules.project.dto.matching;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 项目匹配请求DTO
 */
@Data
public class ProjectMatchRequest {
    
    /**
     * 项目ID
     */
    private Long projectId;
    
    /**
     * 项目数据
     */
    private Map<String, Object> project;
    
    /**
     * 候选人数据列表
     */
    private List<Map<String, Object>> candidates;
}
