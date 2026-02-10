package com.teamup.server.modules.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.api.Result;
import com.teamup.server.modules.mentor.entity.MentorPerformance;
import com.teamup.server.modules.mentor.mapper.MentorPerformanceMapper;
import com.teamup.server.modules.mentor.vo.MentorCardVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
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
@RequestMapping("/api/mentor-relationships")
@RequiredArgsConstructor
public class MentorRelationshipController {

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
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
        
        // 1. 查询所有导师用户
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getRole, "MENTOR");
        
        Page<User> userPage = new Page<>(page, size);
        userMapper.selectPage(userPage, userQuery);
        
        List<User> mentorUsers = userPage.getRecords();
        log.info("查询到 {} 位导师用户", mentorUsers.size());
        
        if (mentorUsers.isEmpty()) {
            Page<MentorCardVO> emptyPage = new Page<>(page, size, 0);
            return Result.success(emptyPage);
        }
        
        // 2. 查询导师的Profile信息
        List<Long> mentorIds = mentorUsers.stream()
            .map(User::getId)
            .collect(Collectors.toList());
        
        LambdaQueryWrapper<UserProfile> profileQuery = new LambdaQueryWrapper<>();
        profileQuery.in(UserProfile::getUserId, mentorIds);
        
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
        
        Map<Long, UserProfile> profileMap = profiles.stream()
            .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        
        // 3. 查询导师绩效数据
        Map<Long, MentorPerformance> performanceMap = performanceMapper.selectList(
            new LambdaQueryWrapper<MentorPerformance>()
                .in(MentorPerformance::getMentorId, mentorIds)
        ).stream().collect(Collectors.toMap(MentorPerformance::getMentorId, p -> p));
        
        // 4. 组装VO（只包含有Profile的导师）
        List<MentorCardVO> mentorCards = mentorUsers.stream()
            .filter(user -> profileMap.containsKey(user.getId()))
            .map(user -> {
                UserProfile profile = profileMap.get(user.getId());
                
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
            .collect(Collectors.toList());
        
        // 5. 构建分页结果
        Page<MentorCardVO> resultPage = new Page<>(page, size, mentorCards.size());
        resultPage.setRecords(mentorCards);
        
        log.info("返回导师列表: total={}, records={}", resultPage.getTotal(), mentorCards.size());
        return Result.success(resultPage);
    }
}
