package com.teamup.server.modules.ecosystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.ecosystem.dto.MomentCreateDTO;
import com.teamup.server.modules.ecosystem.vo.MomentVO;

/**
 * 动态服务接口
 */
public interface MomentService {
    
    /**
     * 创建动态
     */
    Long createMoment(MomentCreateDTO dto, Long userId);
    
    /**
     * 获取动态列表（分页）
     */
    Page<MomentVO> getMomentList(Integer page, Integer size, String type, Long userId);
    
    /**
     * 点赞动态
     */
    void likeMoment(Long id, Long userId);
    
    /**
     * 取消点赞
     */
    void unlikeMoment(Long id, Long userId);
    
    /**
     * 删除动态
     */
    void deleteMoment(Long id, Long userId);
    
    /**
     * 自动创建项目相关动态
     */
    void autoCreateProjectMoment(String type, Long userId, Long projectId, String content);
}
