package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.api.ApiErrorCode;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.user.enums.CreditLevel;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserSkill;
import com.teamup.server.modules.user.entity.UserCredit;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.mapper.UserSkillMapper;
import com.teamup.server.modules.user.mapper.UserCreditMapper;
import com.teamup.server.modules.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户档案服务实现
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileMapper userProfileMapper;
    private final UserSkillMapper userSkillMapper;
    private final UserCreditMapper userCreditMapper;

    @Override
    public UserProfile getProfileByUserId(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile profile = userProfileMapper.selectOne(wrapper);
        
        // 自动修复 NULL 字段，确保前端不会因为 NULL 值导致问题
        if (profile != null) {
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
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId);
        return userSkillMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserSkill(Long userId, UserSkill skill) {
        // 检查是否已存在相同技能
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId)
               .eq(UserSkill::getSkillName, skill.getSkillName());
        
        if (userSkillMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ApiErrorCode.FAILED, "该技能已存在");
        }
        
        skill.setUserId(userId);
        skill.setCreatedAt(LocalDateTime.now());
        userSkillMapper.insert(skill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserSkill(Long skillId) {
        userSkillMapper.deleteById(skillId);
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

