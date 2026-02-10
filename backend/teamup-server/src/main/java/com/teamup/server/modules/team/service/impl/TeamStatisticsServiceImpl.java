package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamup.server.modules.activity.entity.TeamActivity;
import com.teamup.server.modules.activity.mapper.TeamActivityMapper;
import com.teamup.server.modules.chat.entity.Message;
import com.teamup.server.modules.chat.mapper.MessageMapper;
import com.teamup.server.modules.file.entity.FileEntity;
import com.teamup.server.modules.file.mapper.FileMapper;
import com.teamup.server.modules.project.entity.ProjectFile;
import com.teamup.server.modules.project.mapper.ProjectFileMapper;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.service.TeamStatisticsService;
import com.teamup.server.modules.team.vo.TeamStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 团队统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamStatisticsServiceImpl implements TeamStatisticsService {
    
    private final TaskMapper taskMapper;
    private final FileMapper fileMapper;
    private final ProjectFileMapper projectFileMapper;
    private final TeamActivityMapper teamActivityMapper;
    private final TeamMapper teamMapper;
    private final MessageMapper messageMapper;
    
    @Override
    public TeamStatisticsVO calculateStatistics(Long teamId) {
        TeamStatisticsVO vo = new TeamStatisticsVO();
        vo.setTaskCompletionRate(calculateTaskCompletionRate(teamId));
        vo.setActiveDays(calculateActiveDays(teamId));
        vo.setMessageCount(countMessages(teamId));
        vo.setFileCount(countFiles(teamId));
        return vo;
    }
    
    @Override
    public int calculateTaskCompletionRate(Long teamId) {
        try {
            // 查询团队的所有任务
            QueryWrapper<Task> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("team_id", teamId);
            List<Task> tasks = taskMapper.selectList(queryWrapper);
            
            if (tasks.isEmpty()) {
                return 0;
            }
            
            // 统计已完成的任务数量
            long completedCount = tasks.stream()
                    .filter(task -> "DONE".equals(task.getStatus()))
                    .count();
            
            // 计算完成率（百分比）
            return (int) ((completedCount * 100) / tasks.size());
        } catch (Exception e) {
            // 如果查询失败，返回0
            log.warn("Failed to calculate task completion rate for team {}: {}", teamId, e.getMessage());
            return 0;
        }
    }
    
    @Override
    public int calculateActiveDays(Long teamId) {
        try {
            // 获取团队信息
            Team team = teamMapper.selectById(teamId);
            if (team == null) {
                return 0;
            }
            
            int activeDays = 1; // 创建当天算1天
            
            // 尝试统计有活动记录的不同日期数
            try {
                QueryWrapper<TeamActivity> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("team_id", teamId);
                queryWrapper.select("DISTINCT DATE(created_at) as active_date");
                List<TeamActivity> activities = teamActivityMapper.selectList(queryWrapper);
                
                if (activities != null && !activities.isEmpty()) {
                    // 统计不同的活动日期数量
                    activeDays = activities.size();
                }
            } catch (Exception e) {
                // team_activities 表不存在或查询失败
                // 回退到基于任务活动计算
                log.debug("Failed to query team activities for team {}, falling back to task-based calculation: {}", 
                        teamId, e.getMessage());
                
                try {
                    // 统计有任务创建/更新的不同日期数
                    QueryWrapper<Task> taskQuery = new QueryWrapper<>();
                    taskQuery.eq("team_id", teamId);
                    List<Task> tasks = taskMapper.selectList(taskQuery);
                    
                    if (tasks != null && !tasks.isEmpty()) {
                        // 使用任务的创建和更新日期统计活跃天数
                        java.util.Set<java.time.LocalDate> activeDates = new java.util.HashSet<>();
                        for (Task task : tasks) {
                            if (task.getCreatedAt() != null) {
                                activeDates.add(task.getCreatedAt().toLocalDate());
                            }
                            if (task.getUpdatedAt() != null) {
                                activeDates.add(task.getUpdatedAt().toLocalDate());
                            }
                        }
                        if (!activeDates.isEmpty()) {
                            activeDays = activeDates.size();
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Failed to calculate active days from tasks: {}", ex.getMessage());
                }
            }
            
            return activeDays;
        } catch (Exception e) {
            log.warn("Failed to calculate active days for team {}: {}", teamId, e.getMessage());
            return 1; // 默认返回1天
        }
    }
    
    @Override
    public int countMessages(Long teamId) {
        try {
            // 统计团队聊天消息数量
            QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("team_id", teamId);
            return Math.toIntExact(messageMapper.selectCount(queryWrapper));
        } catch (Exception e) {
            // 如果 messages 表不存在或查询失败，返回0
            log.debug("Failed to count messages for team {}: {}", teamId, e.getMessage());
            return 0;
        }
    }
    
    @Override
    public int countFiles(Long teamId) {
        try {
            // 需要通过team的projectId来查询文件
            Team team = teamMapper.selectById(teamId);
            if (team == null || team.getProjectId() == null) {
                return 0;
            }
            
            QueryWrapper<ProjectFile> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", team.getProjectId());
            return Math.toIntExact(projectFileMapper.selectCount(queryWrapper));
        } catch (Exception e) {
            // 如果查询失败，返回0
            log.warn("Failed to count files for team {}: {}", teamId, e.getMessage());
            return 0;
        }
    }
}
