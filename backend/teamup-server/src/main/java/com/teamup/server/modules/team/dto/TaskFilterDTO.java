package com.teamup.server.modules.team.dto;

import lombok.Data;

/**
 * 任务筛选DTO
 * Requirements: 2.1, 10.1, 10.2
 */
@Data
public class TaskFilterDTO {
    /**
     * 任务状态筛选 (TODO, DOING, REVIEW, DONE)
     */
    private String status;
    
    /**
     * 任务优先级筛选 (LOW, MEDIUM, HIGH)
     */
    private String priority;
    
    /**
     * 负责人ID筛选
     */
    private Long assigneeId;
    
    /**
     * 关键词搜索(标题)
     */
    private String keyword;
}
