package com.teamup.server.modules.stats.dto;

import lombok.Data;
import java.util.List;

/**
 * 统计总览DTO
 */
@Data
public class StatsOverviewDTO {
    private Long totalProjects;
    private Long totalUsers;
    private Long totalTeams;
    private Long totalMessages;
    private Long activeUsers;  // 活跃用户数（最近30天有活动的用户）
    private Long completedProjects;  // 已完成项目数
    private List<TrendDataDTO> projectTrend;
    private List<StatusDataDTO> projectStatus;
    private List<ActiveUserDTO> topActiveUsers;  // 活跃用户排行
    private List<DepartmentStatsDTO> departmentStats;  // 院系统计数据
}
