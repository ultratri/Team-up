package com.teamup.server.modules.tag.service;

import com.teamup.server.modules.tag.dto.AddUserSkillDTO;
import com.teamup.server.modules.tag.dto.AddUserTagDTO;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import com.teamup.server.modules.tag.vo.UserTagVO;

import java.util.List;

/**
 * 用户标签服务接口
 */
public interface UserTagService {
    
    /**
     * 获取用户的技能标签
     */
    List<UserSkillVO> getUserSkills(Long userId);
    
    /**
     * 添加用户技能标签
     */
    void addUserSkill(Long userId, AddUserSkillDTO dto);
    
    /**
     * 删除用户技能标签
     */
    void removeUserSkill(Long userTagId);
    
    /**
     * 获取用户指定分类的标签（通用）
     */
    List<UserTagVO> getUserTagsByCategory(Long userId, Tag.TagCategory category);
    
    /**
     * 添加用户标签（通用）
     */
    void addUserTag(Long userId, AddUserTagDTO dto);
    
    /**
     * 删除用户标签（通用）
     */
    void removeUserTag(Long userTagId);
}
