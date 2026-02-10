package com.teamup.server.modules.ecosystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.ecosystem.dto.ResourceCreateDTO;
import com.teamup.server.modules.ecosystem.vo.ResourceVO;

/**
 * 资源服务接口
 */
public interface ResourceService {
    
    /**
     * 创建资源
     */
    Long createResource(ResourceCreateDTO dto, Long userId);
    
    /**
     * 获取资源列表（分页）
     */
    Page<ResourceVO> getResourceList(Integer page, Integer size, String type, String sortBy, Long userId);
    
    /**
     * 获取资源详情
     */
    ResourceVO getResourceDetail(Long id, Long userId);
    
    /**
     * 点赞资源
     */
    void likeResource(Long id, Long userId);
    
    /**
     * 取消点赞
     */
    void unlikeResource(Long id, Long userId);
    
    /**
     * 删除资源
     */
    void deleteResource(Long id, Long userId);
}
