package com.teamup.server.modules.mentor.service;

/**
 * 导师绩效服务接口
 */
public interface MentorPerformanceService {
    
    /**
     * 更新所有导师评分
     */
    void updateAllMentorRatings();
    
    /**
     * 更新单个导师评分
     */
    void updateMentorRating(Long mentorId);
}
