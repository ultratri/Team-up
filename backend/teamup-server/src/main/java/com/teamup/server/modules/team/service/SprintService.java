package com.teamup.server.modules.team.service;

import com.teamup.server.modules.team.entity.Sprint;
import com.teamup.server.modules.team.vo.SprintVO;
import java.util.List;

/**
 * Sprint服务接口
 */
public interface SprintService {
    
    /**
     * 创建Sprint
     */
    Sprint createSprint(Sprint sprint);
    
    /**
     * 更新Sprint
     */
    Sprint updateSprint(Sprint sprint);
    
    /**
     * 删除Sprint
     */
    void deleteSprint(Long id);
    
    /**
     * 获取团队的所有Sprint
     */
    List<SprintVO> getTeamSprints(Long teamId);
    
    /**
     * 获取Sprint详情
     */
    SprintVO getSprintDetail(Long id);
    
    /**
     * 开始Sprint
     */
    void startSprint(Long id);
    
    /**
     * 完成Sprint
     */
    void completeSprint(Long id);
}
