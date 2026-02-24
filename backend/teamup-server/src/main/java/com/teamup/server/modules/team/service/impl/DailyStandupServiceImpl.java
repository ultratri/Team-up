package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.team.entity.DailyStandup;
import com.teamup.server.modules.team.entity.Sprint;
import com.teamup.server.modules.team.mapper.DailyStandupMapper;
import com.teamup.server.modules.team.mapper.SprintMapper;
import com.teamup.server.modules.team.service.DailyStandupService;
import com.teamup.server.modules.team.vo.DailyStandupVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 每日站会服务实现类
 */
@Service
@RequiredArgsConstructor
public class DailyStandupServiceImpl implements DailyStandupService {
    
    private final DailyStandupMapper standupMapper;
    private final UserMapper userMapper;
    private final SprintMapper sprintMapper;
    private final ProfileService profileService;
    
    @Override
    @Transactional
    public DailyStandup submitStandup(DailyStandup standup) {
        // 检查是否已经提交过
        LambdaQueryWrapper<DailyStandup> query = new LambdaQueryWrapper<>();
        query.eq(DailyStandup::getTeamId, standup.getTeamId())
             .eq(DailyStandup::getUserId, standup.getUserId())
             .eq(DailyStandup::getStandupDate, standup.getStandupDate());
        
        DailyStandup existing = standupMapper.selectOne(query);
        if (existing != null) {
            throw new BusinessException("今日已提交站会记录");
        }
        
        standupMapper.insert(standup);
        return standup;
    }
    
    @Override
    @Transactional
    public DailyStandup updateStandup(DailyStandup standup) {
        DailyStandup existing = standupMapper.selectById(standup.getId());
        if (existing == null) {
            throw new BusinessException("站会记录不存在");
        }
        
        standupMapper.updateById(standup);
        return standup;
    }
    
    @Override
    public List<DailyStandupVO> getTeamStandups(Long teamId, LocalDate date) {
        LambdaQueryWrapper<DailyStandup> query = new LambdaQueryWrapper<>();
        query.eq(DailyStandup::getTeamId, teamId)
             .eq(DailyStandup::getStandupDate, date)
             .orderByDesc(DailyStandup::getCreatedAt);
        
        List<DailyStandup> standups = standupMapper.selectList(query);
        
        return standups.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public List<DailyStandupVO> getUserStandups(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DailyStandup> query = new LambdaQueryWrapper<>();
        query.eq(DailyStandup::getUserId, userId)
             .ge(DailyStandup::getStandupDate, startDate)
             .le(DailyStandup::getStandupDate, endDate)
             .orderByDesc(DailyStandup::getStandupDate);
        
        List<DailyStandup> standups = standupMapper.selectList(query);
        
        return standups.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    private DailyStandupVO convertToVO(DailyStandup standup) {
        DailyStandupVO vo = new DailyStandupVO();
        BeanUtils.copyProperties(standup, vo);
        
        // 获取用户信息
        User user = userMapper.selectById(standup.getUserId());
        if (user != null) {
            vo.setUserName(user.getUsername());
            // 从UserProfile获取头像
            UserProfile profile = profileService.getProfileByUserId(user.getId());
            if (profile != null) {
                vo.setUserAvatar(profile.getAvatarUrl());
            }
        }
        
        // 获取Sprint信息
        if (standup.getSprintId() != null) {
            Sprint sprint = sprintMapper.selectById(standup.getSprintId());
            if (sprint != null) {
                vo.setSprintName(sprint.getName());
            }
        }
        
        return vo;
    }
}
