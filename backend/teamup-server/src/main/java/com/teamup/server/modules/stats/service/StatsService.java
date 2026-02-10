package com.teamup.server.modules.stats.service;

import com.teamup.server.modules.stats.dto.StatsOverviewDTO;

/**
 * 统计服务接口
 */
public interface StatsService {
    /**
     * 获取统计总览
     */
    StatsOverviewDTO getOverview();
}
