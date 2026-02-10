package com.teamup.server.modules.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.activity.entity.TeamActivity;
import com.teamup.server.modules.activity.entity.UserActivity;
import com.teamup.server.modules.activity.mapper.TeamActivityMapper;
import com.teamup.server.modules.activity.mapper.UserActivityMapper;
import com.teamup.server.modules.activity.service.ActivityService;
import com.teamup.server.modules.activity.vo.ActivityVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final UserActivityMapper activityMapper;
    private final TeamActivityMapper teamActivityMapper;
    private final UserMapper userMapper;

    @Override
    @Async
    public void logActivity(Long userId, String activityType, String description,
                           String relatedType, Long relatedId, HttpServletRequest request) {
        try {
            UserActivity activity = new UserActivity();
            activity.setUserId(userId);
            activity.setActivityType(activityType);
            activity.setDescription(description);
            activity.setRelatedType(relatedType);
            activity.setRelatedId(relatedId);
            activity.setIpAddress(getClientIP(request));
            activity.setUserAgent(request.getHeader("User-Agent"));
            activity.setCreatedAt(LocalDateTime.now());
            
            activityMapper.insert(activity);
        } catch (Exception e) {
            log.error("记录用户活动失败", e);
            // 不影响主流程，静默失败
        }
    }

    @Override
    public Page<UserActivity> getUserActivities(Long userId, int page, int size, String activityType) {
        Page<UserActivity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserActivity::getUserId, userId);
        
        if (StringUtils.hasText(activityType)) {
            wrapper.eq(UserActivity::getActivityType, activityType);
        }
        
        wrapper.orderByDesc(UserActivity::getCreatedAt);
        
        return activityMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<Long> getRecentActiveUsers(int limit) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        
        LambdaQueryWrapper<UserActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(UserActivity::getCreatedAt, cutoffTime)
               .select(UserActivity::getUserId)
               .groupBy(UserActivity::getUserId)
               .orderByDesc(UserActivity::getCreatedAt)
               .last("LIMIT " + limit);
        
        return activityMapper.selectList(wrapper).stream()
                .map(UserActivity::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Long getUserActivityCount(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<UserActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserActivity::getUserId, userId);
        
        if (startTime != null) {
            wrapper.ge(UserActivity::getCreatedAt, startTime);
        }
        
        if (endTime != null) {
            wrapper.le(UserActivity::getCreatedAt, endTime);
        }
        
        return activityMapper.selectCount(wrapper);
    }

    // ===== 团队活动记录方法实现 =====

    @Override
    public List<ActivityVO> getRecentActivities(Long teamId, Integer limit) {
        LambdaQueryWrapper<TeamActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamActivity::getTeamId, teamId)
               .orderByDesc(TeamActivity::getCreatedAt);
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        List<TeamActivity> activities = teamActivityMapper.selectList(wrapper);
        
        return activities.stream().map(activity -> {
            ActivityVO vo = new ActivityVO();
            BeanUtils.copyProperties(activity, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Async
    public void trackTaskActivity(Long teamId, Long userId, String action, String detail, Long taskId) {
        try {
            TeamActivity activity = createTeamActivity(teamId, userId, "task", action, detail, taskId);
            teamActivityMapper.insert(activity);
        } catch (Exception e) {
            log.error("记录任务活动失败", e);
        }
    }

    @Override
    @Async
    public void trackFileActivity(Long teamId, Long userId, String action, String detail, Long fileId) {
        try {
            TeamActivity activity = createTeamActivity(teamId, userId, "file", action, detail, fileId);
            teamActivityMapper.insert(activity);
        } catch (Exception e) {
            log.error("记录文件活动失败", e);
        }
    }

    @Override
    @Async
    public void trackMessageActivity(Long teamId, Long userId, String detail) {
        try {
            TeamActivity activity = createTeamActivity(teamId, userId, "message", "send", detail, null);
            teamActivityMapper.insert(activity);
        } catch (Exception e) {
            log.error("记录消息活动失败", e);
        }
    }

    @Override
    @Async
    public void trackMemberActivity(Long teamId, Long userId, String action, String detail) {
        try {
            TeamActivity activity = createTeamActivity(teamId, userId, "member", action, detail, null);
            teamActivityMapper.insert(activity);
        } catch (Exception e) {
            log.error("记录成员活动失败", e);
        }
    }

    @Override
    @Async
    public void trackSettingActivity(Long teamId, Long userId, String detail) {
        try {
            TeamActivity activity = createTeamActivity(teamId, userId, "setting", "update", detail, null);
            teamActivityMapper.insert(activity);
        } catch (Exception e) {
            log.error("记录设置活动失败", e);
        }
    }

    /**
     * 创建团队活动记录
     */
    private TeamActivity createTeamActivity(Long teamId, Long userId, String activityType, 
                                           String action, String detail, Long relatedId) {
        TeamActivity activity = new TeamActivity();
        activity.setTeamId(teamId);
        activity.setUserId(userId);
        activity.setActivityType(activityType);
        activity.setAction(action);
        activity.setDetail(detail);
        activity.setRelatedId(relatedId);
        activity.setCreatedAt(LocalDateTime.now());
        
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user != null) {
            activity.setUsername(user.getUsername());
            // 如果有用户档案，获取头像
            // 这里简化处理，实际可能需要关联查询 user_profiles 表
        }
        
        return activity;
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
