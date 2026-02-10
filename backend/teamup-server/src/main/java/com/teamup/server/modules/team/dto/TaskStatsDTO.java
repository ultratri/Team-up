package com.teamup.server.modules.team.dto;

import lombok.Data;

/**
 * 任务统计DTO
 * Requirements: 2.1, 10.1, 10.2
 */
@Data
public class TaskStatsDTO {
    /**
     * TODO状态任务数量
     */
    private Integer todoCount;
    
    /**
     * DOING状态任务数量
     */
    private Integer doingCount;
    
    /**
     * REVIEW状态任务数量
     */
    private Integer reviewCount;
    
    /**
     * DONE状态任务数量
     */
    private Integer doneCount;
    
    /**
     * 总任务数量
     */
    private Integer totalCount;
    
    /**
     * 完成率(百分比)
     */
    private Double completionRate;
    
    /**
     * 逾期任务数量
     */
    private Integer overdueCount;
}
