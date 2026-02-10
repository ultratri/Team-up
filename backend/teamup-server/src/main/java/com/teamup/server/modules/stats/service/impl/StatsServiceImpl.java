package com.teamup.server.modules.stats.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.activity.entity.UserActivity;
import com.teamup.server.modules.activity.mapper.UserActivityMapper;
import com.teamup.server.modules.message.entity.ChatMessage;
import com.teamup.server.modules.message.mapper.ChatMessageMapper;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.stats.dto.*;
import com.teamup.server.modules.stats.service.StatsService;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final ChatMessageMapper messageMapper;
    private final UserActivityMapper activityMapper;
    private final TeamMapper teamMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public StatsOverviewDTO getOverview() {
        StatsOverviewDTO overview = new StatsOverviewDTO();

        try {
            // 总项目数
            overview.setTotalProjects(projectMapper.selectCount(null));
        } catch (Exception e) {
            log.warn("Failed to count projects: {}", e.getMessage());
            overview.setTotalProjects(0L);
        }

        try {
            // 总用户数
            overview.setTotalUsers(userMapper.selectCount(null));
        } catch (Exception e) {
            log.warn("Failed to count users: {}", e.getMessage());
            overview.setTotalUsers(0L);
        }

        try {
            // 总团队数
            overview.setTotalTeams(teamMapper.selectCount(null));
        } catch (Exception e) {
            log.warn("Failed to count teams: {}", e.getMessage());
            overview.setTotalTeams(0L);
        }

        try {
            // 总消息数
            overview.setTotalMessages(messageMapper.selectCount(null));
        } catch (Exception e) {
            log.warn("Failed to count messages: {}", e.getMessage());
            overview.setTotalMessages(0L);
        }

        try {
            // 项目增长趋势（最近7天）
            overview.setProjectTrend(getProjectTrend(7));
        } catch (Exception e) {
            log.warn("Failed to get project trend: {}", e.getMessage());
            overview.setProjectTrend(new ArrayList<>());
        }

        try {
            // 项目状态分布
            overview.setProjectStatus(getProjectStatus());
        } catch (Exception e) {
            log.warn("Failed to get project status: {}", e.getMessage());
            overview.setProjectStatus(new ArrayList<>());
        }

        try {
            // 活跃用户数（最近30天有活动的用户）
            overview.setActiveUsers(getActiveUserCount(30));
        } catch (Exception e) {
            log.warn("Failed to get active user count: {}", e.getMessage());
            overview.setActiveUsers(0L);
        }

        try {
            // 已完成项目数
            LambdaQueryWrapper<Project> completedWrapper = new LambdaQueryWrapper<>();
            completedWrapper.eq(Project::getStatus, "COMPLETED");
            overview.setCompletedProjects(projectMapper.selectCount(completedWrapper));
        } catch (Exception e) {
            log.warn("Failed to count completed projects: {}", e.getMessage());
            overview.setCompletedProjects(0L);
        }

        try {
            // 活跃用户排行（Top 5）
            overview.setTopActiveUsers(getActiveUsers(5));
        } catch (Exception e) {
            log.warn("Failed to get active users: {}", e.getMessage());
            overview.setTopActiveUsers(new ArrayList<>());
        }

        try {
            // 院系统计数据
            overview.setDepartmentStats(getDepartmentStats());
        } catch (Exception e) {
            log.warn("Failed to get department stats: {}", e.getMessage());
            overview.setDepartmentStats(new ArrayList<>());
        }

        return overview;
    }

    /**
     * 获取项目增长趋势
     */
    private List<TrendDataDTO> getProjectTrend(int days) {
        List<TrendDataDTO> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Project::getCreatedAt, startOfDay)
                   .lt(Project::getCreatedAt, endOfDay);

            long count = projectMapper.selectCount(wrapper);

            TrendDataDTO data = new TrendDataDTO();
            data.setDate(date.format(formatter));
            data.setCount(count);
            trend.add(data);
        }

        return trend;
    }

    /**
     * 获取项目状态分布
     */
    private List<StatusDataDTO> getProjectStatus() {
        List<StatusDataDTO> statusList = new ArrayList<>();

        String[] statuses = {"DRAFT", "RECRUITING", "IN_PROGRESS", "COMPLETED"};
        String[] names = {"草稿", "招募中", "进行中", "已完成"};

        for (int i = 0; i < statuses.length; i++) {
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Project::getStatus, statuses[i]);
            long count = projectMapper.selectCount(wrapper);

            if (count > 0) {
                StatusDataDTO data = new StatusDataDTO();
                data.setName(names[i]);
                data.setValue(count);
                statusList.add(data);
            }
        }

        return statusList;
    }

    /**
     * 获取活跃用户排行
     */
    private List<ActiveUserDTO> getActiveUsers(int limit) {
        List<ActiveUserDTO> activeUsers = new ArrayList<>();

        // 查询最近30天的用户活动
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<UserActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(UserActivity::getCreatedAt, thirtyDaysAgo);

        List<UserActivity> activities = activityMapper.selectList(wrapper);

        // 统计每个用户的活动次数
        Map<Long, Long> userActivityCount = new HashMap<>();
        for (UserActivity activity : activities) {
            userActivityCount.merge(activity.getUserId(), 1L, Long::sum);
        }

        // 排序并取前N名
        userActivityCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .forEach(entry -> {
                    User user = userMapper.selectById(entry.getKey());
                    if (user != null) {
                        ActiveUserDTO dto = new ActiveUserDTO();
                        dto.setUserId(user.getId());
                        dto.setName(user.getUsername());
                        dto.setCount(entry.getValue());
                        activeUsers.add(dto);
                    }
                });

        return activeUsers;
    }

    /**
     * 获取活跃用户数（指定天数内有活动的用户）
     */
    private Long getActiveUserCount(int days) {
        LocalDateTime daysAgo = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<UserActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(UserActivity::getCreatedAt, daysAgo);
        
        List<UserActivity> activities = activityMapper.selectList(wrapper);
        return (long) activities.stream()
                .map(UserActivity::getUserId)
                .distinct()
                .count();
    }

    /**
     * 获取院系统计数据
     */
    private List<DepartmentStatsDTO> getDepartmentStats() {
        List<DepartmentStatsDTO> departmentStats = new ArrayList<>();
        
        // 获取所有有院系信息的用户档案
        LambdaQueryWrapper<UserProfile> profileWrapper = new LambdaQueryWrapper<>();
        profileWrapper.isNotNull(UserProfile::getDepartment)
                     .ne(UserProfile::getDepartment, "");
        List<UserProfile> profiles = userProfileMapper.selectList(profileWrapper);
        
        // 统计每个院系的用户数
        Map<String, Long> departmentUserCount = new HashMap<>();
        for (UserProfile profile : profiles) {
            String dept = profile.getDepartment();
            if (dept != null && !dept.isEmpty()) {
                departmentUserCount.merge(dept, 1L, Long::sum);
            }
        }
        
        // 统计每个院系的项目数（通过创建者的院系）
        Map<String, Long> departmentProjectCount = new HashMap<>();
        List<Project> projects = projectMapper.selectList(null);
        for (Project project : projects) {
            UserProfile creatorProfile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, project.getCreatorId())
            );
            if (creatorProfile != null && creatorProfile.getDepartment() != null 
                && !creatorProfile.getDepartment().isEmpty()) {
                String dept = creatorProfile.getDepartment();
                departmentProjectCount.merge(dept, 1L, Long::sum);
            }
        }
        
        // 合并所有院系
        Map<String, DepartmentStatsDTO> deptMap = new HashMap<>();
        for (String dept : departmentUserCount.keySet()) {
            DepartmentStatsDTO dto = new DepartmentStatsDTO();
            dto.setDepartment(dept);
            dto.setUserCount(departmentUserCount.get(dept));
            dto.setProjectCount(departmentProjectCount.getOrDefault(dept, 0L));
            deptMap.put(dept, dto);
        }
        
        // 添加只有项目但没有用户的院系
        for (String dept : departmentProjectCount.keySet()) {
            if (!deptMap.containsKey(dept)) {
                DepartmentStatsDTO dto = new DepartmentStatsDTO();
                dto.setDepartment(dept);
                dto.setUserCount(0L);
                dto.setProjectCount(departmentProjectCount.get(dept));
                deptMap.put(dept, dto);
            }
        }
        
        // 转换为列表并按用户数排序
        departmentStats.addAll(deptMap.values());
        departmentStats.sort((a, b) -> Long.compare(b.getUserCount(), a.getUserCount()));
        
        // 只返回前10个院系
        return departmentStats.stream().limit(10).collect(java.util.stream.Collectors.toList());
    }
}
