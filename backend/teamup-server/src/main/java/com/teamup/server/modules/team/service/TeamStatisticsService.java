package com.teamup.server.modules.team.service;

import com.teamup.server.modules.team.vo.TeamStatisticsVO;

/**
 * 团队统计服务接口
 */
public interface TeamStatisticsService {
    /**
     * 计算团队统计数据
     * @param teamId 团队ID
     * @return 团队统计数据
     */
    TeamStatisticsVO calculateStatistics(Long teamId);
    
    /**
     * 计算任务完成率
     * @param teamId 团队ID
     * @return 任务完成率（百分比）
     */
    int calculateTaskCompletionRate(Long teamId);
    
    /**
     * 计算活跃天数
     * @param teamId 团队ID
     * @return 活跃天数
     */
    int calculateActiveDays(Long teamId);
    
    /**
     * 统计消息数量
     * @param teamId 团队ID
     * @return 消息总数
     */
    int countMessages(Long teamId);
    
    /**
     * 统计文件数量
     * @param teamId 团队ID
     * @return 文件总数
     */
    int countFiles(Long teamId);
}
