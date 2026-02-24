package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserAvailability;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserAvailabilityMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import com.teamup.server.modules.user.service.TalentService;
import com.teamup.server.modules.user.vo.TalentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人才墙服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TalentServiceImpl implements TalentService {

    private final UserMapper userMapper;
    private final UserAvailabilityMapper availabilityMapper;
    private final UserProfileMapper profileMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public Page<TalentVO> getTalentList(int page, int size, String department, String keyword, String intention) {
        // 1. 先查询所有 isAvailable = true 的用户ID
        LambdaQueryWrapper<UserAvailability> availQuery = new LambdaQueryWrapper<>();
        availQuery.eq(UserAvailability::getIsAvailable, true);
        
        // 如果有组队意向筛选
        if (StringUtils.hasText(intention)) {
            availQuery.like(UserAvailability::getIntention, intention);
        }
        
        List<UserAvailability> availabilities = availabilityMapper.selectList(availQuery);
        
        // 如果没有可用的用户，直接返回空结果
        if (availabilities.isEmpty()) {
            Page<TalentVO> result = new Page<>(page, size);
            result.setTotal(0);
            result.setRecords(new ArrayList<>());
            return result;
        }
        
        List<Long> availableUserIds = availabilities.stream()
            .map(UserAvailability::getUserId)
            .collect(Collectors.toList());
        
        Map<Long, UserAvailability> availMap = availabilities.stream()
            .collect(Collectors.toMap(UserAvailability::getUserId, a -> a));
        
        // 2. 查询这些用户的基本信息（状态为 ACTIVE）
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.in(User::getId, availableUserIds);
        userQuery.eq(User::getStatus, "ACTIVE");
        
        // 如果有关键词，搜索用户名
        if (StringUtils.hasText(keyword)) {
            userQuery.like(User::getUsername, keyword);
        }
        
        Page<User> userPage = new Page<>(page, size);
        userPage = userMapper.selectPage(userPage, userQuery);
        
        List<Long> userIds = userPage.getRecords().stream()
            .map(User::getId)
            .collect(Collectors.toList());
        
        if (userIds.isEmpty()) {
            Page<TalentVO> result = new Page<>(page, size);
            result.setTotal(0);
            result.setRecords(new ArrayList<>());
            return result;
        }
        
        // 3. 查询用户档案（必须有档案才显示在人才墙）
        LambdaQueryWrapper<UserProfile> profileQuery = new LambdaQueryWrapper<>();
        profileQuery.in(UserProfile::getUserId, userIds);
        profileQuery.isNotNull(UserProfile::getRealName); // 必须有真实姓名
        
        // 如果有院系筛选
        if (StringUtils.hasText(department)) {
            profileQuery.eq(UserProfile::getDepartment, department);
        }
        
        List<UserProfile> profiles = profileMapper.selectList(profileQuery);
        Map<Long, UserProfile> profileMap = profiles.stream()
            .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        
        // 4. 查询所有用户的角色，用于过滤导师
        Map<Long, List<String>> userRolesMap = userIds.stream()
            .collect(Collectors.toMap(
                userId -> userId,
                userId -> userRoleMapper.getUserRoles(userId)
            ));
        
        // 5. 组装 TalentVO（只包含学生，排除导师）
        List<TalentVO> talents = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            UserProfile profile = profileMap.get(user.getId());
            
            // 必须有档案信息才显示
            if (profile == null) {
                continue;
            }
            
            // 排除导师（通过检查用户角色表）
            List<String> roles = userRolesMap.get(user.getId());
            if (roles != null && roles.contains("MENTOR")) {
                continue;
            }
            
            UserAvailability avail = availMap.get(user.getId());
            
            // 必须有可用性信息（因为我们是从可用性表开始查询的）
            if (avail == null) {
                continue;
            }
            
            TalentVO vo = new TalentVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
            vo.setRealName(profile.getRealName());
            vo.setDepartment(profile.getDepartment());
            vo.setMajor(profile.getMajor());
            vo.setGrade(profile.getGrade());
            vo.setAvatarUrl(profile.getAvatarUrl());
            vo.setBio(profile.getBio());
            vo.setWechat(profile.getWechat());
            vo.setQq(profile.getQq());
            vo.setPhone(user.getPhone());
            vo.setIsAvailable(avail.getIsAvailable());
            vo.setIntentions(parseIntentions(avail.getIntention()));
            vo.setWeeklyHours(avail.getWeeklyHours());
            
            talents.add(vo);
        }
        
        // 6. 返回结果
        Page<TalentVO> result = new Page<>(page, size);
        result.setTotal(talents.size()); // 注意：这里的 total 是过滤后的数量
        result.setRecords(talents);
        
        return result;
    }
    
    /**
     * 解析组队意向字符串为列表
     */
    private List<String> parseIntentions(String intentions) {
        if (!StringUtils.hasText(intentions)) {
            return new ArrayList<>();
        }
        
        // 假设存储格式为逗号分隔的字符串
        String[] parts = intentions.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
