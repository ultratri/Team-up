package com.teamup.server.modules.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.activity.entity.UserActivity;
import com.teamup.server.modules.activity.vo.ActivityVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 活动记录服务接口
 */
public interface ActivityService {
    /**
     * 记录用户活动
     */
    void logActivity(Long userId, String activityType, String description,
                    String relatedType, Long relatedId, HttpServletRequest request);
    
    /**
     * 获取用户活动列表
     */
    Page<UserActivity> getUserActivities(Long userId, int page, int size, String activityType);
    
    /**
     * 获取最近活跃用户
     */
    java.util.List<Long> getRecentActiveUsers(int limit);
    
    /**
     * 统计用户活跃度
     */
    Long getUserActivityCount(Long userId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);
    
    // ===== 团队活动记录方法 =====
    
    /**
     * 获取团队最近活动记录
     */
    List<ActivityVO> getRecentActivities(Long teamId, Integer limit);
    
    /**
     * 记录任务相关活动
     */
    void trackTaskActivity(Long teamId, Long userId, String action, String detail, Long taskId);
    
    /**
     * 记录文件相关活动
     */
    void trackFileActivity(Long teamId, Long userId, String action, String detail, Long fileId);
    
    /**
     * 记录消息相关活动
     */
    void trackMessageActivity(Long teamId, Long userId, String detail);
    
    /**
     * 记录成员相关活动
     */
    void trackMemberActivity(Long teamId, Long userId, String action, String detail);
    
    /**
     * 记录设置相关活动
     */
    void trackSettingActivity(Long teamId, Long userId, String detail);
}
