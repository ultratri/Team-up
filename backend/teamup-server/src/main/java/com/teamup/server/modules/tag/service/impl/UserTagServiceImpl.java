package com.teamup.server.modules.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.tag.dto.AddUserSkillDTO;
import com.teamup.server.modules.tag.dto.AddUserTagDTO;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.entity.UserTag;
import com.teamup.server.modules.tag.mapper.TagMapper;
import com.teamup.server.modules.tag.mapper.UserTagMapper;
import com.teamup.server.modules.tag.service.UserTagService;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import com.teamup.server.modules.tag.vo.UserTagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户标签服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTagServiceImpl extends ServiceImpl<UserTagMapper, UserTag> implements UserTagService {
    
    private final UserTagMapper userTagMapper;
    private final TagMapper tagMapper;
    
    @Override
    public List<UserSkillVO> getUserSkills(Long userId) {
        return userTagMapper.selectUserSkills(userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserSkill(Long userId, AddUserSkillDTO dto) {
        log.info("添加用户技能: userId={}, tagId={}", userId, dto.getTagId());
        
        // 检查标签是否存在且为技能标签
        Tag tag = tagMapper.selectById(dto.getTagId());
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        if (!"SKILL".equals(tag.getCategory())) {
            throw new BusinessException("只能添加技能类标签");
        }
        if (!"ACTIVE".equals(tag.getStatus())) {
            throw new BusinessException("该标签已被禁用");
        }
        
        // 检查是否已添加
        LambdaQueryWrapper<UserTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTag::getUserId, userId)
               .eq(UserTag::getTagId, dto.getTagId());
        
        if (userTagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该技能已添加");
        }
        
        // 创建用户标签关联
        UserTag userTag = new UserTag();
        userTag.setUserId(userId);
        userTag.setTagId(dto.getTagId());
        userTag.setProficiencyLevel(dto.getProficiencyLevel());
        userTag.setIsVerified(false);
        
        userTagMapper.insert(userTag);
        
        // 更新标签使用次数
        tag.setUsageCount(tag.getUsageCount() + 1);
        tagMapper.updateById(tag);
        
        log.info("用户技能添加成功: userTagId={}", userTag.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserSkill(Long userTagId) {
        log.info("删除用户技能: userTagId={}", userTagId);
        
        UserTag userTag = userTagMapper.selectById(userTagId);
        if (userTag == null) {
            throw new BusinessException("用户技能不存在");
        }
        
        // 删除用户标签关联
        userTagMapper.deleteById(userTagId);
        
        // 更新标签使用次数
        Tag tag = tagMapper.selectById(userTag.getTagId());
        if (tag != null && tag.getUsageCount() > 0) {
            tag.setUsageCount(tag.getUsageCount() - 1);
            tagMapper.updateById(tag);
        }
        
        log.info("用户技能删除成功");
    }
    
    @Override
    public List<UserTagVO> getUserTagsByCategory(Long userId, String category) {
        return userTagMapper.selectUserTagsByCategory(userId, category);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserTag(Long userId, AddUserTagDTO dto) {
        log.info("添加用户标签: userId={}, tagId={}", userId, dto.getTagId());
        
        // 检查标签是否存在
        Tag tag = tagMapper.selectById(dto.getTagId());
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        if (!"ACTIVE".equals(tag.getStatus())) {
            throw new BusinessException("该标签已被禁用");
        }
        
        // 检查是否已添加
        LambdaQueryWrapper<UserTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTag::getUserId, userId)
               .eq(UserTag::getTagId, dto.getTagId());
        
        if (userTagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该标签已添加");
        }
        
        // 创建用户标签关联
        UserTag userTag = new UserTag();
        userTag.setUserId(userId);
        userTag.setTagId(dto.getTagId());
        userTag.setProficiencyLevel(dto.getProficiencyLevel());
        userTag.setIsVerified(false);
        
        userTagMapper.insert(userTag);
        
        // 更新标签使用次数
        tag.setUsageCount(tag.getUsageCount() + 1);
        tagMapper.updateById(tag);
        
        log.info("用户标签添加成功: userTagId={}", userTag.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserTag(Long userTagId) {
        log.info("删除用户标签: userTagId={}", userTagId);
        
        UserTag userTag = userTagMapper.selectById(userTagId);
        if (userTag == null) {
            throw new BusinessException("用户标签不存在");
        }
        
        // 删除用户标签关联
        userTagMapper.deleteById(userTagId);
        
        // 更新标签使用次数
        Tag tag = tagMapper.selectById(userTag.getTagId());
        if (tag != null && tag.getUsageCount() > 0) {
            tag.setUsageCount(tag.getUsageCount() - 1);
            tagMapper.updateById(tag);
        }
        
        log.info("用户标签删除成功");
    }
}
