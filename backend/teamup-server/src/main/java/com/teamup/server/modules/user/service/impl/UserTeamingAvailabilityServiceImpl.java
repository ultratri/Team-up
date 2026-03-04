package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.user.dto.UpdateUserTeamingAvailabilityDTO;
import com.teamup.server.modules.user.entity.UserTeamingAvailability;
import com.teamup.server.modules.user.mapper.UserTeamingAvailabilityMapper;
import com.teamup.server.modules.user.service.UserTeamingAvailabilityService;
import com.teamup.server.modules.user.vo.UserTeamingAvailabilityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户组队可用性服务实现
 */
@Service
@RequiredArgsConstructor
public class UserTeamingAvailabilityServiceImpl implements UserTeamingAvailabilityService {
    
    private final UserTeamingAvailabilityMapper teamingAvailabilityMapper;
    
    @Override
    public UserTeamingAvailabilityVO getUserTeamingAvailability(Long userId) {
        UserTeamingAvailability entity = teamingAvailabilityMapper.selectByUserId(userId);
        
        if (entity == null) {
            // 如果没有记录，返回默认值
            UserTeamingAvailabilityVO vo = new UserTeamingAvailabilityVO();
            vo.setUserId(userId);
            vo.setIsAvailable(false);
            vo.setVisibility("PUBLIC");
            return vo;
        }
        
        UserTeamingAvailabilityVO vo = new UserTeamingAvailabilityVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
    
    @Override
    public void updateUserTeamingAvailability(Long userId, UpdateUserTeamingAvailabilityDTO dto) {
        UserTeamingAvailability existing = teamingAvailabilityMapper.selectByUserId(userId);
        
        if (existing == null) {
            // 创建新记录
            UserTeamingAvailability entity = new UserTeamingAvailability();
            entity.setUserId(userId);
            entity.setIsAvailable(dto.getIsAvailable());
            // 将intentions列表转换为逗号分隔的字符串
            if (dto.getIntentions() != null && !dto.getIntentions().isEmpty()) {
                entity.setIntention(String.join(",", dto.getIntentions()));
            }
            entity.setVisibility(dto.getVisibility());
            entity.setAvailableFrom(dto.getAvailableFrom());
            entity.setAvailableUntil(dto.getAvailableUntil());
            entity.setWeeklyHours(dto.getWeeklyHours());
            entity.setNotes(dto.getNotes());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            teamingAvailabilityMapper.insert(entity);
        } else {
            // 更新现有记录
            existing.setIsAvailable(dto.getIsAvailable());
            // 将intentions列表转换为逗号分隔的字符串
            if (dto.getIntentions() != null && !dto.getIntentions().isEmpty()) {
                existing.setIntention(String.join(",", dto.getIntentions()));
            } else {
                existing.setIntention(null);
            }
            existing.setVisibility(dto.getVisibility());
            existing.setAvailableFrom(dto.getAvailableFrom());
            existing.setAvailableUntil(dto.getAvailableUntil());
            existing.setWeeklyHours(dto.getWeeklyHours());
            existing.setNotes(dto.getNotes());
            existing.setUpdatedAt(LocalDateTime.now());
            teamingAvailabilityMapper.updateById(existing);
        }
    }
}
