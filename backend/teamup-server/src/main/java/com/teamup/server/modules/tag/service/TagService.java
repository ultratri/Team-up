package com.teamup.server.modules.tag.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.tag.dto.CreateTagDTO;
import com.teamup.server.modules.tag.dto.MergeTagDTO;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.vo.TagUsageVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {
    
    /**
     * 创建标签
     */
    Long createTag(Long creatorId, CreateTagDTO dto);
    
    /**
     * 更新标签
     */
    void updateTag(Long tagId, CreateTagDTO dto);
    
    /**
     * 删除标签
     */
    void deleteTag(Long tagId);
    
    /**
     * 合并标签
     */
    void mergeTags(MergeTagDTO dto);
    
    /**
     * 查询标签列表
     */
    Page<Tag> listTags(int page, int size, Tag.TagCategory category, String keyword);
    
    /**
     * 查询标签详情
     */
    Tag getTagDetail(Long tagId);
    
    /**
     * 查询标签使用统计
     */
    List<TagUsageVO> getTagUsageStatistics(int limit);
    
    /**
     * 查询热门标签
     */
    List<Tag> getPopularTags(Tag.TagCategory category, int limit);
}
