package com.teamup.server.modules.ecosystem.vo;

import lombok.Data;

/**
 * 生态广场统计数据VO
 */
@Data
public class EcosystemStatsVO {
    
    /**
     * 活跃项目数（招募中+进行中）
     */
    private Long projectCount;
    
    /**
     * 在线人才数（活跃用户数）
     */
    private Long talentCount;
    
    /**
     * 进行中比赛数
     */
    private Long competitionCount;
}
