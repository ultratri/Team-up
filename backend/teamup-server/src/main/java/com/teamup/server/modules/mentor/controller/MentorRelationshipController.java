package com.teamup.server.modules.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.api.Result;
import com.teamup.server.modules.mentor.entity.MentorPerformance;
import com.teamup.server.modules.mentor.mapper.MentorPerformanceMapper;
import com.teamup.server.modules.mentor.vo.MentorCardVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserRole;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导师关系控制器
 * 提供导师列表查询等功能
 */
@Slf4j
@RestController
@RequestMapping("/mentor-relationships")
@RequiredArgsConstructor
public class MentorRelationshipController {

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final UserRoleMapper roleMapper;
    private final MentorPerformanceMapper performanceMapper;

    /**
     * 获取导师列表（学员可见）
     * 
     * @param page 页码
     * @param size 每页大小
     * @param department 院系筛选
     * @param keyword 关键词搜索
     * @return 导师列表
     */
    @GetMapping("/mentors")
    public Result<Page<MentorCardVO>> getMentorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String keyword) {
        
        log.info("获取导师列表: page={}, size={}, department={}, keyword={}", page, size, department, keyword);
        
        // 1. 从 user_roles 表查询所有导师用户ID
        List<Long> mentorUserIds = roleMapper.selectList(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleName, "MENTOR")
        ).stream().map(UserRole::getUserId).collect(Collectors.toList());
        
        if (mentorUserIds.isEmpty()) {
            log.info("没有找到导师用户");
            Page<MentorCardVO> emptyPage = new Page<>(page, size, 0);
            return Result.success(emptyPage);
        }
        
        log.info("找到 {} 位导师用户ID", mentorUserIds.size());
        
        // 2. 查询导师的Profile信息（带筛选条件）
        LambdaQueryWrapper<UserProfile> profileQuery = new LambdaQueryWrapper<>();
        profileQuery.in(UserProfile::getUserId, mentorUserIds);
        
        if (StringUtils.hasText(department)) {
            profileQuery.eq(UserProfile::getDepartment, department);
        }
        
        if (StringUtils.hasText(keyword)) {
            profileQuery.and(wrapper -> wrapper
                .like(UserProfile::getRealName, keyword)
                .or().like(UserProfile::getBio, keyword)
            );
        }
        
        List<UserProfile> profiles = profileMapper.selectList(profileQuery);
        log.info("查询到 {} 条导师Profile", profiles.size());
        
        if (profiles.isEmpty()) {
            Page<MentorCardVO> emptyPage = new Page<>(page, size, 0);
            return Result.success(emptyPage);
        }
        
        // 3. 获取这些Profile对应的用户ID
        List<Long> profileUserIds = profiles.stream()
            .map(UserProfile::getUserId)
            .collect(Collectors.toList());
        
        // 4. 查询用户基本信息
        Map<Long, User> userMap = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .in(User::getId, profileUserIds)
        ).stream().collect(Collectors.toMap(User::getId, u -> u));
        
        // 5. 查询导师绩效数据
        Map<Long, MentorPerformance> performanceMap = performanceMapper.selectList(
            new LambdaQueryWrapper<MentorPerformance>()
                .in(MentorPerformance::getMentorId, profileUserIds)
        ).stream().collect(Collectors.toMap(MentorPerformance::getMentorId, p -> p));
        
        // 6. 组装VO
        List<MentorCardVO> mentorCards = profiles.stream()
            .map(profile -> {
                User user = userMap.get(profile.getUserId());
                if (user == null) {
                    return null;
                }
                
                MentorCardVO vo = new MentorCardVO();
                vo.setId(user.getId());
                vo.setUsername(user.getUsername());
                vo.setRealName(profile.getRealName());
                vo.setAvatar(profile.getAvatarUrl());
                vo.setAvatarUrl(profile.getAvatarUrl());
                vo.setDepartment(profile.getDepartment());
                vo.setMajor(profile.getMajor());
                vo.setBio(profile.getBio());
                vo.setProjectExperience(profile.getProjectExperience());
                vo.setGuidanceExperience(profile.getGuidanceExperience());
                
                // 设置绩效数据
                MentorPerformance performance = performanceMap.get(user.getId());
                if (performance != null) {
                    vo.setTotalMentees(performance.getTotalMentees() != null ? performance.getTotalMentees() : 0);
                    vo.setActiveMentees(performance.getActiveMentees() != null ? performance.getActiveMentees() : 0);
                    vo.setSuccessfulMentees(performance.getSuccessfulMentees() != null ? performance.getSuccessfulMentees() : 0);
                    vo.setTotalRewardPoints(performance.getTotalRewardPoints() != null ? performance.getTotalRewardPoints() : 0);
                    vo.setRating(performance.getRating() != null ? performance.getRating().doubleValue() : 5.0);
                } else {
                    // 默认值
                    vo.setTotalMentees(0);
                    vo.setActiveMentees(0);
                    vo.setSuccessfulMentees(0);
                    vo.setTotalRewardPoints(0);
                    vo.setRating(5.0);
                }
                
                vo.setAvailable(true);
                
                return vo;
            })
            .filter(vo -> vo != null)
            .collect(Collectors.toList());
        
        // 7. 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, mentorCards.size());
        List<MentorCardVO> pagedCards = mentorCards.subList(start, end);
        
        // 8. 构建分页结果
        Page<MentorCardVO> resultPage = new Page<>(page, size, mentorCards.size());
        resultPage.setRecords(pagedCards);
        
        log.info("返回导师列表: total={}, records={}", resultPage.getTotal(), pagedCards.size());
        return Result.success(resultPage);
    }
}
