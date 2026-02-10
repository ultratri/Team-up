package com.teamup.server.modules.team.vo;

import lombok.Data;

/**
 * 团队统计数据VO
 */
@Data
public class TeamStatisticsVO {
    /**
     * 任务完成率（百分比）
     */
    private Integer taskCompletionRate;
    
    /**
     * 活跃天数
     */
    private Integer activeDays;
    
    /**
     * 消息总数
     */
    private Integer messageCount;
    
    /**
     * 文件总数
     */
    private Integer fileCount;
}
