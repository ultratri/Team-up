package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.tag.entity.UserTag;
import com.teamup.server.modules.tag.mapper.UserTagMapper;
import com.teamup.server.modules.user.dto.UserAvailabilityRequest;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserAvailability;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserAvailabilityMapper;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.service.UserAvailabilityService;
import com.teamup.server.modules.user.vo.UserAvailabilityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用户可用性服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvailabilityServiceImpl implements UserAvailabilityService {
    
    private final UserAvailabilityMapper availabilityMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final UserTagMapper tagMapper;
    
    @Override
    public UserAvailabilityVO getUserAvailability(Long userId) {
        UserAvailability availability = availabilityMapper.selectByUserId(userId);
        
        if (availability == null) {
            // 返回默认值
            return UserAvailabilityVO.builder()
                .isAvailable(false)
                .intentions(Collections.emptyList())
                .visibility("PUBLIC")
                .build();
        }
        
        return convertToVO(availability);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "talentList", allEntries = true)  // 清除人才列表缓存
    public void updateAvailability(Long userId, UserAvailabilityRequest request) {
        // 验证用户存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 如果要上墙，验证资格
        if (request.getIsAvailable()) {
            validateQualification(userId);
        }
        
        // 查询现有记录
        UserAvailability existing = availabilityMapper.selectByUserId(userId);
        
        if (existing == null) {
            // 创建新记录
            UserAvailability availability = new UserAvailability();
            availability.setUserId(userId);
            availability.setIsAvailable(request.getIsAvailable());
            availability.setIntention(request.getIntentions() != null ? 
                String.join(",", request.getIntentions()) : null);
            availability.setVisibility(request.getVisibility());
            availability.setAvailableFrom(request.getAvailableFrom());
            availability.setAvailableUntil(request.getAvailableUntil());
            availability.setWeeklyHours(request.getWeeklyHours());
            availability.setNotes(request.getNotes());
            availability.setCreatedAt(LocalDateTime.now());
            availability.setUpdatedAt(LocalDateTime.now());
            
            availabilityMapper.insert(availability);
            log.info("创建用户组队意向记录，用户ID: {}", userId);
        } else {
            // 更新现有记录
            existing.setIsAvailable(request.getIsAvailable());
            existing.setIntention(request.getIntentions() != null ? 
                String.join(",", request.getIntentions()) : null);
            existing.setVisibility(request.getVisibility());
            existing.setAvailableFrom(request.getAvailableFrom());
            existing.setAvailableUntil(request.getAvailableUntil());
            existing.setWeeklyHours(request.getWeeklyHours());
            existing.setNotes(request.getNotes());
            existing.setUpdatedAt(LocalDateTime.now());
            
            availabilityMapper.updateById(existing);
            log.info("更新用户组队意向记录，用户ID: {}", userId);
        }
    }
    
    /**
     * 验证用户是否满足上墙资格
     */
    private void validateQualification(Long userId) {
        // 1. 验证基本信息完整
        UserProfile profile = profileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
        );
        
        if (profile == null ||
            !StringUtils.hasText(profile.getRealName()) ||
            !StringUtils.hasText(profile.getDepartment()) ||
            !StringUtils.hasText(profile.getMajor())) {
            throw new BusinessException("请先完善基本信息（真实姓名、院系、专业）");
        }
        
        // 2. 验证至少有1个技能标签
        long tagCount = tagMapper.selectCount(
            new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId)
        );
        
        if (tagCount == 0) {
            throw new BusinessException("请先添加至少1个技能标签");
        }
        
        // 3. 验证账号状态
        User user = userMapper.selectById(userId);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("账号状态异常，无法上墙");
        }
    }
    
    /**
     * 将实体转换为VO
     */
    private UserAvailabilityVO convertToVO(UserAvailability availability) {
        List<String> intentions = Collections.emptyList();
        if (StringUtils.hasText(availability.getIntention())) {
            intentions = Arrays.asList(availability.getIntention().split(","));
        }
        
        return UserAvailabilityVO.builder()
            .isAvailable(availability.getIsAvailable())
            .intentions(intentions)
            .visibility(availability.getVisibility())
            .availableFrom(availability.getAvailableFrom())
            .availableUntil(availability.getAvailableUntil())
            .weeklyHours(availability.getWeeklyHours())
            .notes(availability.getNotes())
            .build();
    }
}
