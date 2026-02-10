package com.teamup.server.modules.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.tag.dto.CreateTagDTO;
import com.teamup.server.modules.tag.dto.MergeTagDTO;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.mapper.TagMapper;
import com.teamup.server.modules.tag.service.TagService;
import com.teamup.server.modules.tag.vo.TagUsageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 标签服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
    
    private final TagMapper tagMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(Long creatorId, CreateTagDTO dto) {
        log.info("创建标签: name={}, category={}", dto.getName(), dto.getCategory());
        
        // 检查标签是否已存在
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, dto.getName())
               .eq(Tag::getCategory, dto.getCategory())
               .eq(Tag::getStatus, Tag.TagStatus.ACTIVE);
        
        if (tagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该标签已存在");
        }
        
        // 创建标签
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tag.setCategory(dto.getCategory());
        tag.setParentId(dto.getParentId());
        tag.setDescription(dto.getDescription());
        tag.setIsOfficial(dto.getIsOfficial() != null ? dto.getIsOfficial() : false);
        tag.setStatus(Tag.TagStatus.ACTIVE);
        tag.setUsageCount(0);
        tag.setCreatedBy(creatorId);
        
        tagMapper.insert(tag);
        log.info("标签创建成功: tagId={}", tag.getId());
        
        return tag.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(Long tagId, CreateTagDTO dto) {
        log.info("更新标签: tagId={}", tagId);
        
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        
        // 检查新名称是否与其他标签冲突
        if (!tag.getName().equals(dto.getName())) {
            LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Tag::getName, dto.getName())
                   .eq(Tag::getCategory, dto.getCategory())
                   .ne(Tag::getId, tagId)
                   .eq(Tag::getStatus, Tag.TagStatus.ACTIVE);
            
            if (tagMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("标签名称已被使用");
            }
        }
        
        tag.setName(dto.getName());
        tag.setCategory(dto.getCategory());
        tag.setParentId(dto.getParentId());
        tag.setDescription(dto.getDescription());
        if (dto.getIsOfficial() != null) {
            tag.setIsOfficial(dto.getIsOfficial());
        }
        
        tagMapper.updateById(tag);
        log.info("标签更新成功: tagId={}", tagId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId) {
        log.info("删除标签: tagId={}", tagId);
        
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        
        // 检查标签是否正在使用
        if (tag.getUsageCount() > 0) {
            throw new BusinessException("该标签正在使用中，无法删除。请先将其合并到其他标签或等待使用结束");
        }
        
        // 软删除：标记为已废弃
        tag.setStatus(Tag.TagStatus.DEPRECATED);
        tagMapper.updateById(tag);
        
        log.info("标签删除成功: tagId={}", tagId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeTags(MergeTagDTO dto) {
        log.info("合并标签: sourceTagId={}, targetTagId={}", dto.getSourceTagId(), dto.getTargetTagId());
        
        Tag sourceTag = tagMapper.selectById(dto.getSourceTagId());
        Tag targetTag = tagMapper.selectById(dto.getTargetTagId());
        
        if (sourceTag == null || targetTag == null) {
            throw new BusinessException("标签不存在");
        }
        
        if (!sourceTag.getCategory().equals(targetTag.getCategory())) {
            throw new BusinessException("只能合并相同分类的标签");
        }
        
        // 更新源标签状态
        sourceTag.setStatus(Tag.TagStatus.MERGED);
        sourceTag.setMergedToId(dto.getTargetTagId());
        tagMapper.updateById(sourceTag);
        
        // 更新目标标签使用次数
        targetTag.setUsageCount(targetTag.getUsageCount() + sourceTag.getUsageCount());
        tagMapper.updateById(targetTag);
        
        // TODO: 更新所有使用源标签的记录，改为使用目标标签
        // 这需要更新 user_tags 和 project_tags 表
        
        log.info("标签合并成功");
    }
    
    @Override
    public Page<Tag> listTags(int page, int size, Tag.TagCategory category, String keyword) {
        Page<Tag> tagPage = new Page<>(page, size);
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        
        if (category != null) {
            wrapper.eq(Tag::getCategory, category);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Tag::getName, keyword);
        }
        
        wrapper.eq(Tag::getStatus, Tag.TagStatus.ACTIVE)
               .orderByDesc(Tag::getUsageCount);
        
        return tagMapper.selectPage(tagPage, wrapper);
    }
    
    @Override
    public Tag getTagDetail(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return tag;
    }
    
    @Override
    public List<TagUsageVO> getTagUsageStatistics(int limit) {
        return tagMapper.selectTagUsageStatistics(limit);
    }
    
    @Override
    public List<Tag> getPopularTags(Tag.TagCategory category, int limit) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        
        if (category != null) {
            wrapper.eq(Tag::getCategory, category);
        }
        
        wrapper.eq(Tag::getStatus, Tag.TagStatus.ACTIVE)
               .orderByDesc(Tag::getUsageCount)
               .last("LIMIT " + limit);
        
        return tagMapper.selectList(wrapper);
    }
}
