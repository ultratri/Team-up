package com.teamup.server.modules.stats.dto;

import lombok.Data;

/**
 * 院系统计数据DTO
 */
@Data
public class DepartmentStatsDTO {
    private String department;
    private Long userCount;
    private Long projectCount;
}
