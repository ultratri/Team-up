package com.teamup.server.modules.ecosystem.service;

import com.teamup.server.modules.ecosystem.vo.EcosystemStatsVO;

/**
 * 生态广场服务接口
 */
public interface EcosystemService {
    
    /**
     * 获取生态广场统计数据
     */
    EcosystemStatsVO getStats();
}
