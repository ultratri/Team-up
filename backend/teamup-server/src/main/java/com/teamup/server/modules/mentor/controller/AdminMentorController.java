package com.teamup.server.modules.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.api.Result;
import com.teamup.server.modules.mentor.entity.MentorPerformance;
import com.teamup.server.modules.mentor.mapper.MentorPerformanceMapper;
import com.teamup.server.modules.mentor.service.MentorPerformanceService;
import com.teamup.server.modules.mentor.vo.MentorDetailVO;
import com.teamup.server.modules.mentor.vo.MentorInfoVO;
import com.teamup.server.modules.mentor.vo.MentorRankingVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserRole;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员导师管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/mentors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminMentorController {

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final UserRoleMapper roleMapper;
    private final MentorPerformanceMapper performanceMapper;
    private final MentorPerformanceService performanceService;

    /**
     * 获取导师列表（管理员）
     */
    @GetMapping
    public Result<Page<MentorInfoVO>> getMentorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("管理员获取导师列表: page={}, size={}", page, size);
        
        // 1. 从 user_roles 表查询所有导师用户ID
        List<Long> mentorUserIds = roleMapper.selectList(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleName, "MENTOR")
        ).stream().map(UserRole::getUserId).collect(Collectors.toList());
        
        if (mentorUserIds.isEmpty()) {
            Page<MentorInfoVO> emptyPage = new Page<>(page, size, 0);
            return Result.success(emptyPage);
        }
        
        // 2. 查询用户基本信息
        Map<Long, User> userMap = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .in(User::getId, mentorUserIds)
        ).stream().collect(Collectors.toMap(User::getId, u -> u));
        
        // 3. 查询用户档案信息
        Map<Long, UserProfile> profileMap = profileMapper.selectList(
            new LambdaQueryWrapper<UserProfile>()
                .in(UserProfile::getUserId, mentorUserIds)
        ).stream().collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        
        // 4. 查询导师绩效数据
        Map<Long, MentorPerformance> performanceMap = performanceMapper.selectList(
            new LambdaQueryWrapper<MentorPerformance>()
                .in(MentorPerformance::getMentorId, mentorUserIds)
        ).stream().collect(Collectors.toMap(MentorPerformance::getMentorId, p -> p));
        
        // 5. 组装VO
        List<MentorInfoVO> mentorList = new ArrayList<>();
        for (Long userId : mentorUserIds) {
            User user = userMap.get(userId);
            if (user == null) continue;
            
            UserProfile profile = profileMap.get(userId);
            MentorPerformance performance = performanceMap.get(userId);
            
            MentorInfoVO vo = new MentorInfoVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(profile != null ? profile.getRealName() : user.getUsername());
            vo.setDepartment(profile != null ? profile.getDepartment() : null);
            vo.setMajor(profile != null ? profile.getMajor() : null);
            
            if (performance != null) {
                vo.setTotalMentees(performance.getTotalMentees() != null ? performance.getTotalMentees() : 0);
                vo.setActiveMentees(performance.getActiveMentees() != null ? performance.getActiveMentees() : 0);
                vo.setCompletedMentees(performance.getCompletedMentees() != null ? performance.getCompletedMentees() : 0);
                vo.setSuccessfulMentees(performance.getSuccessfulMentees() != null ? performance.getSuccessfulMentees() : 0);
                vo.setAverageMenteeScore(performance.getAverageMenteeScore() != null ? performance.getAverageMenteeScore().doubleValue() : 0.0);
                vo.setTotalRewardPoints(performance.getTotalRewardPoints() != null ? performance.getTotalRewardPoints() : 0);
                vo.setRating(performance.getRating() != null ? performance.getRating().doubleValue() : 5.0);
            } else {
                vo.setTotalMentees(0);
                vo.setActiveMentees(0);
                vo.setCompletedMentees(0);
                vo.setSuccessfulMentees(0);
                vo.setAverageMenteeScore(0.0);
                vo.setTotalRewardPoints(0);
                vo.setRating(5.0);
            }
            
            mentorList.add(vo);
        }
        
        // 6. 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, mentorList.size());
        List<MentorInfoVO> pagedList = mentorList.subList(start, end);
        
        Page<MentorInfoVO> resultPage = new Page<>(page, size, mentorList.size());
        resultPage.setRecords(pagedList);
        
        return Result.success(resultPage);
    }

    /**
     * 获取导师绩效排行榜
     */
    @GetMapping("/ranking")
    public Result<List<MentorRankingVO>> getMentorRanking(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("获取导师排行榜: limit={}", limit);
        
        // 查询所有导师绩效，按评分排序
        List<MentorPerformance> performances = performanceMapper.selectList(
            new LambdaQueryWrapper<MentorPerformance>()
                .orderByDesc(MentorPerformance::getRating)
                .orderByDesc(MentorPerformance::getSuccessfulMentees)
                .last("LIMIT " + limit)
        );
        
        if (performances.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 查询用户信息
        List<Long> mentorIds = performances.stream()
            .map(MentorPerformance::getMentorId)
            .collect(Collectors.toList());
        
        Map<Long, User> userMap = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .in(User::getId, mentorIds)
        ).stream().collect(Collectors.toMap(User::getId, u -> u));
        
        Map<Long, UserProfile> profileMap = profileMapper.selectList(
            new LambdaQueryWrapper<UserProfile>()
                .in(UserProfile::getUserId, mentorIds)
        ).stream().collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        
        // 组装排行榜VO
        List<MentorRankingVO> rankings = new ArrayList<>();
        int rank = 1;
        for (MentorPerformance performance : performances) {
            User user = userMap.get(performance.getMentorId());
            if (user == null) continue;
            
            UserProfile profile = profileMap.get(performance.getMentorId());
            
            MentorRankingVO vo = new MentorRankingVO();
            vo.setRank(rank++);
            vo.setMentorId(user.getId());
            vo.setMentorName(profile != null ? profile.getRealName() : user.getUsername());
            vo.setDepartment(profile != null ? profile.getDepartment() : null);
            vo.setSuccessfulMentees(performance.getSuccessfulMentees() != null ? performance.getSuccessfulMentees() : 0);
            vo.setAverageMenteeScore(performance.getAverageMenteeScore() != null ? performance.getAverageMenteeScore().doubleValue() : 0.0);
            vo.setTotalRewardPoints(performance.getTotalRewardPoints() != null ? performance.getTotalRewardPoints() : 0);
            vo.setRating(performance.getRating() != null ? performance.getRating().doubleValue() : 5.0);
            
            rankings.add(vo);
        }
        
        return Result.success(rankings);
    }

    /**
     * 撤销导师资格
     */
    @PostMapping("/{mentorId}/revoke")
    public Result<String> revokeMentor(@PathVariable Long mentorId) {
        log.info("撤销导师资格: mentorId={}", mentorId);
        
        // 删除导师角色
        roleMapper.delete(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, mentorId)
                .eq(UserRole::getRoleName, "MENTOR")
        );
        
        return Result.success("导师资格已撤销");
    }

    /**
     * 获取导师详情
     */
    @GetMapping("/{mentorId}")
    public Result<MentorDetailVO> getMentorDetail(@PathVariable Long mentorId) {
        log.info("获取导师详情: mentorId={}", mentorId);
        
        User user = userMapper.selectById(mentorId);
        if (user == null) {
            return Result.error(404, "导师不存在");
        }
        
        UserProfile profile = profileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, mentorId)
        );
        
        MentorPerformance performance = performanceMapper.selectOne(
            new LambdaQueryWrapper<MentorPerformance>()
                .eq(MentorPerformance::getMentorId, mentorId)
        );
        
        MentorDetailVO vo = new MentorDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(profile != null ? profile.getRealName() : user.getUsername());
        vo.setDepartment(profile != null ? profile.getDepartment() : null);
        vo.setMajor(profile != null ? profile.getMajor() : null);
        vo.setEmail(user.getEmail());
        vo.setPhone(null); // UserProfile没有phone字段
        vo.setBio(profile != null ? profile.getBio() : null);
        
        if (performance != null) {
            vo.setTotalMentees(performance.getTotalMentees() != null ? performance.getTotalMentees() : 0);
            vo.setActiveMentees(performance.getActiveMentees() != null ? performance.getActiveMentees() : 0);
            vo.setCompletedMentees(performance.getCompletedMentees() != null ? performance.getCompletedMentees() : 0);
            vo.setSuccessfulMentees(performance.getSuccessfulMentees() != null ? performance.getSuccessfulMentees() : 0);
            vo.setAverageMenteeScore(performance.getAverageMenteeScore() != null ? performance.getAverageMenteeScore().doubleValue() : 0.0);
            vo.setTotalRewardPoints(performance.getTotalRewardPoints() != null ? performance.getTotalRewardPoints() : 0);
            vo.setRating(performance.getRating() != null ? performance.getRating().doubleValue() : 5.0);
        } else {
            vo.setTotalMentees(0);
            vo.setActiveMentees(0);
            vo.setCompletedMentees(0);
            vo.setSuccessfulMentees(0);
            vo.setAverageMenteeScore(0.0);
            vo.setTotalRewardPoints(0);
            vo.setRating(5.0);
        }
        
        return Result.success(vo);
    }

    /**
     * 更新所有导师评分
     */
    @PostMapping("/update-ratings")
    public Result<String> updateAllMentorRatings() {
        log.info("更新所有导师评分");
        
        try {
            performanceService.updateAllMentorRatings();
            return Result.success("所有导师评分已更新");
        } catch (Exception e) {
            log.error("更新导师评分失败", e);
            return Result.error(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 更新单个导师评分
     */
    @PostMapping("/{mentorId}/update-rating")
    public Result<String> updateMentorRating(@PathVariable Long mentorId) {
        log.info("更新导师评分: mentorId={}", mentorId);
        
        try {
            performanceService.updateMentorRating(mentorId);
            return Result.success("导师评分已更新");
        } catch (Exception e) {
            log.error("更新导师评分失败", e);
            return Result.error(500, "更新失败: " + e.getMessage());
        }
    }
}
