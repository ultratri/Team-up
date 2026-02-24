package com.teamup.server.modules.team.service;

import com.teamup.server.modules.team.entity.DailyStandup;
import com.teamup.server.modules.team.vo.DailyStandupVO;
import java.time.LocalDate;
import java.util.List;

/**
 * 每日站会服务接口
 */
public interface DailyStandupService {
    
    /**
     * 提交站会记录
     */
    DailyStandup submitStandup(DailyStandup standup);
    
    /**
     * 更新站会记录
     */
    DailyStandup updateStandup(DailyStandup standup);
    
    /**
     * 获取团队某日的站会记录
     */
    List<DailyStandupVO> getTeamStandups(Long teamId, LocalDate date);
    
    /**
     * 获取用户的站会记录
     */
    List<DailyStandupVO> getUserStandups(Long userId, LocalDate startDate, LocalDate endDate);
}
