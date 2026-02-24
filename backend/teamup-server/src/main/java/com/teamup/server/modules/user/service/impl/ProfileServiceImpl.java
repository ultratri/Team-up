package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.api.ApiErrorCode;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.user.enums.CreditLevel;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserSkill;
import com.teamup.server.modules.user.entity.UserCredit;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.mapper.UserCreditMapper;
import com.teamup.server.modules.user.service.ProfileService;
import com.teamup.server.modules.tag.service.UserTagService;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户档案服务实现
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileMapper userProfileMapper;
    private final UserCreditMapper userCreditMapper;
    private final UserTagService userTagService;

    @Override
    public UserProfile getProfileByUserId(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile profile = userProfileMapper.selectOne(wrapper);
        
        // 如果用户还没有 profile 记录，返回一个默认的空对象（不入库）
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setRealName("");
            profile.setDepartment("");
            profile.setMajor("");
            profile.setGrade(1);
            profile.setWechat("");
            profile.setQq("");
            profile.setBio("");
            profile.setProjectExperience("");
            profile.setGuidanceExperience("");
            return profile;
        }
        
        // 自动修复 NULL 字段，确保前端不会因为 NULL 值导致问题
        if (profile.getBio() == null) {
            profile.setBio("");
        }
        if (profile.getProjectExperience() == null) {
            profile.setProjectExperience("");
        }
        if (profile.getGuidanceExperience() == null) {
            profile.setGuidanceExperience("");
        }
        if (profile.getRealName() == null) {
            profile.setRealName("");
        }
        if (profile.getDepartment() == null) {
            profile.setDepartment("");
        }
        if (profile.getMajor() == null) {
            profile.setMajor("");
        }
        if (profile.getWechat() == null) {
            profile.setWechat("");
        }
        if (profile.getQq() == null) {
            profile.setQq("");
        }
        
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UserProfile profile) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile existingProfile = userProfileMapper.selectOne(wrapper);
        
        if (existingProfile != null) {
            profile.setId(existingProfile.getId());
            profile.setUserId(userId);
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileMapper.updateById(profile);
        } else {
            profile.setUserId(userId);
            profile.setCreatedAt(LocalDateTime.now());
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileMapper.insert(profile);
        }
    }

    @Override
    public List<UserSkill> getUserSkills(Long userId) {
        // 使用新的 UserTagService 获取技能标签
        List<UserSkillVO> skillVOs = userTagService.getUserSkills(userId);
        
        // 转换为 UserSkill 对象（为了保持接口兼容性）
        return skillVOs.stream()
            .map(vo -> {
                UserSkill skill = new UserSkill();
                skill.setId(vo.getId());
                skill.setUserId(userId);
                skill.setSkillName(vo.getTagName());
                skill.setSkillCategory("SKILL");  // 固定为 SKILL 类别
                skill.setProficiencyLevel(vo.getProficiencyLevel());
                skill.setCreatedAt(LocalDateTime.now());  // 使用当前时间
                return skill;
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserSkill(Long userId, UserSkill skill) {
        // 将 UserSkill 转换为 AddUserSkillDTO
        // 注意：这里需要先根据 skillName 查找对应的 tagId
        // 如果找不到，则抛出异常
        throw new BusinessException(ApiErrorCode.FAILED, 
            "该方法已废弃，请使用 UserTagService.addUserSkill 方法，并提供 tagId");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserSkill(Long skillId) {
        // 委托给 UserTagService 处理
        userTagService.removeUserSkill(skillId);
    }

    @Override
    public UserCredit getUserCredit(Long userId) {
        LambdaQueryWrapper<UserCredit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCredit::getUserId, userId);
        UserCredit credit = userCreditMapper.selectOne(wrapper);
        
        // 如果没有信誉记录，返回默认对象（暂不入库，遵循CQS原则）
        if (credit == null) {
            credit = new UserCredit();
            credit.setUserId(userId);
            credit.setTotalCredit(0);
            credit.setCreditLevel(CreditLevel.NEWBIE.getCode());
            credit.setUpdatedAt(LocalDateTime.now());
        }
        
        return credit;
    }
}

