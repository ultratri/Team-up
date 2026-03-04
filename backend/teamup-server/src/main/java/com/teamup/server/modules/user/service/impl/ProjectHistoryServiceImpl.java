package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.teamup.server.modules.evaluation.entity.Evaluation;
import com.teamup.server.modules.evaluation.mapper.EvaluationMapper;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.user.dto.ExperienceScore;
import com.teamup.server.modules.user.entity.UserProjectHistory;
import com.teamup.server.modules.user.mapper.UserProjectHistoryMapper;
import com.teamup.server.modules.user.service.ProjectHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目履历服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectHistoryServiceImpl implements ProjectHistoryService {
    
    private final UserProjectHistoryMapper historyMapper;
    private final ProjectMapper projectMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final EvaluationMapper evaluationMapper;
    
    @Override
    public List<UserProjectHistory> getUserProjectHistory(Long userId, boolean onlyCompleted) {
        LambdaQueryWrapper<UserProjectHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProjectHistory::getUserId, userId);
        
        if (onlyCompleted) {
            wrapper.eq(UserProjectHistory::getIsCompleted, true);
        }
        
        wrapper.orderByDesc(UserProjectHistory::getCompletedAt)
               .orderByDesc(UserProjectHistory::getJoinedAt);
        
        return historyMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public void onProjectCompleted(Long projectId) {
        log.info("项目完成，开始创建履历记录: projectId={}", projectId);
        
        // 获取项目信息
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            log.warn("项目不存在: projectId={}", projectId);
            return;
        }
        
        // 获取项目关联的团队ID
        Long teamId = project.getTeamId();
        if (teamId == null) {
            log.warn("项目没有关联团队: projectId={}", projectId);
            return;
        }
        
        // 获取团队所有成员
        LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamMember::getTeamId, teamId);
        List<TeamMember> members = teamMemberMapper.selectList(memberWrapper);
        
        log.info("找到 {} 个团队成员", members.size());
        
        for (TeamMember member : members) {
            createOrUpdateHistory(member, project);
        }
    }
    
    @Override
    @Transactional
    public void onUserJoinedTeam(Long userId, Long teamId, Long projectId, String role) {
        log.info("用户加入团队，创建履历记录: userId={}, teamId={}, projectId={}", 
                 userId, teamId, projectId);
        
        // 检查是否已存在
        LambdaQueryWrapper<UserProjectHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProjectHistory::getUserId, userId)
               .eq(UserProjectHistory::getProjectId, projectId);
        
        UserProjectHistory existing = historyMapper.selectOne(wrapper);
        if (existing != null) {
            log.info("履历记录已存在，跳过创建");
            return;
        }
        
        // 获取项目信息
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            log.warn("项目不存在: projectId={}", projectId);
            return;
        }
        
        // 创建新履历
        UserProjectHistory history = new UserProjectHistory();
        history.setUserId(userId);
        history.setProjectId(projectId);
        history.setTeamId(teamId);
        history.setRole(role);
        history.setJoinedAt(LocalDateTime.now());
        history.setIsCompleted(false);
        history.setProjectTitle(project.getTitle());
        history.setProjectType(project.getProjectType());
        history.setProjectDescription(project.getDescription());
        history.setIsVerified(true);
        history.setVerificationSource("SYSTEM");
        history.setEvaluationCount(0);
        
        historyMapper.insert(history);
        log.info("履历记录创建成功");
    }
    
    @Override
    @Transactional
    public void onEvaluationReceived(Long projectId, Long evaluatedUserId) {
        log.info("收到评价，更新履历: projectId={}, userId={}", projectId, evaluatedUserId);
        
        // 查找履历记录
        LambdaQueryWrapper<UserProjectHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProjectHistory::getUserId, evaluatedUserId)
               .eq(UserProjectHistory::getProjectId, projectId);
        
        UserProjectHistory history = historyMapper.selectOne(wrapper);
        if (history == null) {
            log.warn("履历记录不存在，跳过更新");
            return;
        }
        
        // 计算评价分数
        updateEvaluationScores(history);
    }
    
    @Override
    @Transactional
    public void syncAllProjectHistory() {
        log.info("开始同步所有项目履历");
        
        // 获取所有履历记录
        List<UserProjectHistory> allHistory = historyMapper.selectList(null);
        
        for (UserProjectHistory history : allHistory) {
            // 更新参与天数
            if (history.getJoinedAt() != null) {
                LocalDateTime endTime = history.getCompletedAt() != null 
                    ? history.getCompletedAt() 
                    : LocalDateTime.now();
                long days = ChronoUnit.DAYS.between(history.getJoinedAt(), endTime);
                history.setDurationDays((int) days);
            }
            
            // 更新评价分数
            updateEvaluationScores(history);
            
            historyMapper.updateById(history);
        }
        
        log.info("项目履历同步完成，共处理 {} 条记录", allHistory.size());
    }
    
    @Override
    public ExperienceScore calculateExperienceScore(Long userId) {
        List<UserProjectHistory> history = getUserProjectHistory(userId, true);
        
        ExperienceScore score = new ExperienceScore();
        score.setIsVerified(!history.isEmpty());
        score.setCompletedProjects(history.size());
        
        if (history.isEmpty()) {
            // 降级到默认分数
            score.setTotalScore(50.0);
            score.setBreakdown(Map.of(
                "project_count", 0.0,
                "quality", 50.0,
                "leadership", 0.0,
                "diversity", 0.0
            ));
            return score;
        }
        
        // 1. 项目数量得分（权重30%）
        int completedCount = history.size();
        double projectScore = Math.min(completedCount * 10.0, 100.0);
        
        // 2. 项目质量得分（权重40%）
        List<Double> qualityScores = new ArrayList<>();
        for (UserProjectHistory h : history) {
            if (h.getEvaluationCount() != null && h.getEvaluationCount() > 0) {
                double avgScore = calculateAvgScore(h);
                qualityScores.add(avgScore / 5.0 * 100.0);  // 转换为百分制
            }
        }
        double qualityScore = qualityScores.isEmpty() 
            ? 50.0 
            : qualityScores.stream().mapToDouble(Double::doubleValue).average().orElse(50.0);
        
        // 3. 领导经验得分（权重15%）
        long leaderCount = history.stream()
            .filter(h -> "LEADER".equals(h.getRole()))
            .count();
        double leadershipScore = Math.min(leaderCount * 20.0, 100.0);
        score.setLeaderProjects((int) leaderCount);
        
        // 4. 项目类型多样性得分（权重15%）
        Set<String> projectTypes = history.stream()
            .map(UserProjectHistory::getProjectType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        double diversityScore = Math.min(projectTypes.size() * 25.0, 100.0);
        score.setProjectTypeDiversity(projectTypes.size());
        
        // 综合计算
        double totalScore = projectScore * 0.30 
                          + qualityScore * 0.40 
                          + leadershipScore * 0.15 
                          + diversityScore * 0.15;
        
        score.setTotalScore(Math.round(totalScore * 100.0) / 100.0);
        score.setBreakdown(Map.of(
            "project_count", Math.round(projectScore * 100.0) / 100.0,
            "quality", Math.round(qualityScore * 100.0) / 100.0,
            "leadership", Math.round(leadershipScore * 100.0) / 100.0,
            "diversity", Math.round(diversityScore * 100.0) / 100.0
        ));
        
        // 计算平均项目评分
        if (!qualityScores.isEmpty()) {
            double avgProjectScore = qualityScores.stream()
                .mapToDouble(s -> s / 100.0 * 5.0)
                .average()
                .orElse(0.0);
            score.setAvgProjectScore(Math.round(avgProjectScore * 100.0) / 100.0);
        }
        
        return score;
    }
    
    /**
     * 创建或更新履历记录
     */
    private void createOrUpdateHistory(TeamMember member, Project project) {
        // 检查是否已存在
        LambdaQueryWrapper<UserProjectHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProjectHistory::getUserId, member.getUserId())
               .eq(UserProjectHistory::getProjectId, project.getId());
        
        UserProjectHistory history = historyMapper.selectOne(wrapper);
        
        if (history == null) {
            // 创建新记录
            history = new UserProjectHistory();
            history.setUserId(member.getUserId());
            history.setProjectId(project.getId());
            history.setTeamId(member.getTeamId());
            history.setRole(member.getRole());
            history.setJoinedAt(member.getJoinedAt());
            history.setProjectTitle(project.getTitle());
            history.setProjectType(project.getProjectType());
            history.setProjectDescription(project.getDescription());
            history.setIsVerified(true);
            history.setVerificationSource("SYSTEM");
            history.setEvaluationCount(0);
        }
        
        // 更新完成信息
        history.setIsCompleted(true);
        history.setCompletedAt(LocalDateTime.now());
        
        // 计算参与天数
        if (history.getJoinedAt() != null) {
            long days = ChronoUnit.DAYS.between(history.getJoinedAt(), LocalDateTime.now());
            history.setDurationDays((int) days);
        }
        
        // 更新评价分数
        updateEvaluationScores(history);
        
        if (history.getId() == null) {
            historyMapper.insert(history);
        } else {
            historyMapper.updateById(history);
        }
        
        log.info("履历记录已更新: userId={}, projectId={}", member.getUserId(), project.getId());
    }
    
    /**
     * 更新评价分数
     */
    private void updateEvaluationScores(UserProjectHistory history) {
        LambdaQueryWrapper<Evaluation> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.eq(Evaluation::getProjectId, history.getProjectId())
                   .eq(Evaluation::getEvaluatedId, history.getUserId());
        
        List<Evaluation> evaluations = evaluationMapper.selectList(evalWrapper);
        
        if (evaluations.isEmpty()) {
            history.setEvaluationCount(0);
            return;
        }
        
        history.setEvaluationCount(evaluations.size());
        
        // 计算平均分
        double avgTech = evaluations.stream()
            .mapToInt(Evaluation::getTechContributionScore)
            .average()
            .orElse(0.0);
        
        double avgCollab = evaluations.stream()
            .mapToInt(Evaluation::getCollaborationScore)
            .average()
            .orElse(0.0);
        
        double avgTask = evaluations.stream()
            .mapToInt(Evaluation::getTaskCompletionScore)
            .average()
            .orElse(0.0);
        
        history.setAvgTechScore(BigDecimal.valueOf(avgTech).setScale(2, RoundingMode.HALF_UP));
        history.setAvgCollaborationScore(BigDecimal.valueOf(avgCollab).setScale(2, RoundingMode.HALF_UP));
        history.setAvgTaskCompletionScore(BigDecimal.valueOf(avgTask).setScale(2, RoundingMode.HALF_UP));
    }
    
    /**
     * 计算综合平均分
     */
    private double calculateAvgScore(UserProjectHistory history) {
        double tech = history.getAvgTechScore() != null 
            ? history.getAvgTechScore().doubleValue() : 0.0;
        double collab = history.getAvgCollaborationScore() != null 
            ? history.getAvgCollaborationScore().doubleValue() : 0.0;
        double task = history.getAvgTaskCompletionScore() != null 
            ? history.getAvgTaskCompletionScore().doubleValue() : 0.0;
        
        return (tech * 0.4 + collab * 0.3 + task * 0.3);
    }
}
