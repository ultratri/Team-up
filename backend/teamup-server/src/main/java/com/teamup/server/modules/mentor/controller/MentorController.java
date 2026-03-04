package com.teamup.server.modules.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.mentor.entity.MentorPerformance;
import com.teamup.server.modules.mentor.mapper.MentorPerformanceMapper;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserRole;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 导师控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
public class MentorController {
    
    private final TeamMemberMapper teamMemberMapper;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final com.teamup.server.modules.user.mapper.UserProfileMapper profileMapper;
    private final MentorPerformanceMapper performanceMapper;
    
    /**
     * 获取我的所有团队（包括我是导师的团队和我是学员的团队）
     */
    @GetMapping("/teams")
    public Result<Map<String, Object>> getMyTeams(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        
        try {
            Long userId = SecurityUtils.getUserId();
            
            // 1. 查询用户作为成员参与的团队
            LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(TeamMember::getUserId, userId);
            List<TeamMember> members = teamMemberMapper.selectList(memberWrapper);
            
            Set<Long> teamIds = members.stream()
                    .map(TeamMember::getTeamId)
                    .collect(Collectors.toSet());
            
            // 2. 查询用户作为导师指导的团队
            LambdaQueryWrapper<Team> mentorTeamWrapper = new LambdaQueryWrapper<>();
            mentorTeamWrapper.eq(Team::getMentorId, userId);
            List<Team> mentorTeams = teamMapper.selectList(mentorTeamWrapper);
            
            // 合并团队ID
            mentorTeams.forEach(team -> teamIds.add(team.getId()));
            
            if (teamIds.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("records", new ArrayList<>());
                result.put("total", 0);
                result.put("current", page);
                result.put("size", size);
                return Result.success(result);
            }
            
            // 3. 查询所有团队信息
            LambdaQueryWrapper<Team> teamWrapper = new LambdaQueryWrapper<>();
            teamWrapper.in(Team::getId, teamIds);
            if (keyword != null && !keyword.trim().isEmpty()) {
                teamWrapper.like(Team::getTeamName, keyword.trim());
            }
            teamWrapper.orderByDesc(Team::getCreatedAt);
            
            List<Team> teams = teamMapper.selectList(teamWrapper);
            
            // 4. 构建返回数据
            List<Map<String, Object>> records = new ArrayList<>();
            for (Team team : teams) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", team.getId());
                record.put("teamName", team.getTeamName());
                record.put("competitionId", team.getCompetitionId());
                
                // 判断当前用户的角色
                boolean isMentor = userId.equals(team.getMentorId());
                if (isMentor) {
                    record.put("myRole", "MENTOR");
                } else {
                    // 查找当前用户在该团队的角色
                    TeamMember myMember = members.stream()
                            .filter(m -> m.getTeamId().equals(team.getId()))
                            .findFirst()
                            .orElse(null);
                    
                    if (myMember != null) {
                        record.put("myRole", myMember.getRole());
                    }
                }
                
                // 获取导师信息
                if (team.getMentorId() != null) {
                    record.put("mentorId", team.getMentorId());
                    User mentor = userMapper.selectById(team.getMentorId());
                    if (mentor != null) {
                        record.put("mentorName", mentor.getUsername());
                    }
                }
                
                records.add(record);
            }
            
            // 5. 分页处理
            int start = (page - 1) * size;
            int end = Math.min(start + size, records.size());
            List<Map<String, Object>> pagedRecords = start < records.size() ? 
                    records.subList(start, end) : new ArrayList<>();
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", pagedRecords);
            result.put("total", records.size());
            result.put("current", page);
            result.put("size", size);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取团队列表失败", e);
            return Result.error(500, "获取团队列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取导师广场列表（学员可见）
     */
    @GetMapping("/plaza")
    public Result<Map<String, Object>> getMentorPlaza(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean availableOnly) {
        
        try {
            log.info("获取导师广场列表: page={}, size={}, department={}, keyword={}", page, size, department, keyword);
            
            // 1. 查询所有导师用户ID
            List<Long> mentorUserIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getRoleName, "MENTOR")
            ).stream().map(UserRole::getUserId).collect(Collectors.toList());
            
            if (mentorUserIds.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("records", new ArrayList<>());
                result.put("total", 0);
                return Result.success(result);
            }
            
            // 2. 查询导师Profile（带筛选）
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
            
            if (profiles.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("records", new ArrayList<>());
                result.put("total", 0);
                return Result.success(result);
            }
            
            // 3. 获取用户和绩效数据
            List<Long> profileUserIds = profiles.stream()
                .map(UserProfile::getUserId)
                .collect(Collectors.toList());
            
            Map<Long, User> userMap = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, profileUserIds)
            ).stream().collect(Collectors.toMap(User::getId, u -> u));
            
            Map<Long, MentorPerformance> performanceMap = performanceMapper.selectList(
                new LambdaQueryWrapper<MentorPerformance>()
                    .in(MentorPerformance::getMentorId, profileUserIds)
            ).stream().collect(Collectors.toMap(MentorPerformance::getMentorId, p -> p));
            
            // 4. 组装数据
            List<Map<String, Object>> mentorCards = profiles.stream()
                .map(profile -> {
                    User user = userMap.get(profile.getUserId());
                    if (user == null) return null;
                    
                    Map<String, Object> card = new HashMap<>();
                    card.put("id", user.getId());
                    card.put("username", user.getUsername());
                    card.put("realName", profile.getRealName());
                    card.put("avatar", profile.getAvatarUrl());
                    card.put("department", profile.getDepartment());
                    card.put("title", profile.getMajor());
                    card.put("bio", profile.getBio());
                    
                    MentorPerformance performance = performanceMap.get(user.getId());
                    if (performance != null) {
                        card.put("rating", performance.getRating() != null ? performance.getRating().doubleValue() : 5.0);
                        card.put("totalStudents", performance.getTotalMentees() != null ? performance.getTotalMentees() : 0);
                        card.put("activeStudents", performance.getActiveMentees() != null ? performance.getActiveMentees() : 0);
                        card.put("contributionPoints", performance.getTotalRewardPoints() != null ? performance.getTotalRewardPoints() : 0);
                    } else {
                        card.put("rating", 5.0);
                        card.put("totalStudents", 0);
                        card.put("activeStudents", 0);
                        card.put("contributionPoints", 0);
                    }
                    
                    card.put("available", true);
                    card.put("specialties", new ArrayList<>());
                    
                    return card;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            // 5. 排序
            if ("students".equals(sortBy)) {
                mentorCards.sort((a, b) -> 
                    Integer.compare((Integer)b.get("totalStudents"), (Integer)a.get("totalStudents")));
            } else if ("points".equals(sortBy)) {
                mentorCards.sort((a, b) -> 
                    Integer.compare((Integer)b.get("contributionPoints"), (Integer)a.get("contributionPoints")));
            } else {
                // 默认按评分排序
                mentorCards.sort((a, b) -> 
                    Double.compare((Double)b.get("rating"), (Double)a.get("rating")));
            }
            
            // 6. 分页
            int start = (page - 1) * size;
            int end = Math.min(start + size, mentorCards.size());
            List<Map<String, Object>> pagedCards = mentorCards.subList(start, end);
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", pagedCards);
            result.put("total", mentorCards.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取导师广场列表失败", e);
            return Result.error(500, "获取导师广场列表失败: " + e.getMessage());
        }
    }
}
