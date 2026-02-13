package com.teamup.server.modules.mentor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.mentor.entity.MentorPerformance;
import com.teamup.server.modules.mentor.mapper.MentorPerformanceMapper;
import com.teamup.server.modules.mentor.service.MentorPerformanceService;
import com.teamup.server.modules.user.entity.UserRole;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 导师绩效服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentorPerformanceServiceImpl implements MentorPerformanceService {
    
    private final MentorPerformanceMapper performanceMapper;
    private final UserRoleMapper roleMapper;
    
    @Override
    public void updateAllMentorRatings() {
        // 获取所有导师ID
        List<Long> mentorIds = roleMapper.selectList(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleName, "MENTOR")
        ).stream().map(UserRole::getUserId).collect(Collectors.toList());
        
        for (Long mentorId : mentorIds) {
            updateMentorRating(mentorId);
        }
        
        log.info("已更新 {} 位导师的评分", mentorIds.size());
    }
    
    @Override
    public void updateMentorRating(Long mentorId) {
        MentorPerformance performance = performanceMapper.selectOne(
            new LambdaQueryWrapper<MentorPerformance>()
                .eq(MentorPerformance::getMentorId, mentorId)
        );
        
        if (performance == null) {
            log.warn("导师绩效记录不存在: mentorId={}", mentorId);
            return;
        }
        
        // 计算评分：基于成功学员数、平均分数、奖励积分
        double rating = calculateRating(performance);
        performance.setRating(BigDecimal.valueOf(rating));
        
        performanceMapper.updateById(performance);
        log.info("已更新导师评分: mentorId={}, rating={}", mentorId, rating);
    }
    
    /**
     * 计算导师评分
     * 评分算法：
     * - 成功学员数权重：40%
     * - 平均学员分数权重：40%
     * - 奖励积分权重：20%
     */
    private double calculateRating(MentorPerformance performance) {
        // 成功学员数得分（最高5分）
        int successfulMentees = performance.getSuccessfulMentees() != null ? performance.getSuccessfulMentees() : 0;
        double successScore = Math.min(5.0, successfulMentees * 0.5);
        
        // 平均学员分数（0-5分）
        double avgScore = performance.getAverageMenteeScore() != null 
            ? performance.getAverageMenteeScore().doubleValue() / 20.0  // 假设原始分数是0-100，转换为0-5
            : 0.0;
        
        // 奖励积分得分（最高5分）
        int rewardPoints = performance.getTotalRewardPoints() != null ? performance.getTotalRewardPoints() : 0;
        double rewardScore = Math.min(5.0, rewardPoints * 0.01);
        
        // 加权计算
        double rating = successScore * 0.4 + avgScore * 0.4 + rewardScore * 0.2;
        
        // 确保评分在0-5之间
        return Math.max(0.0, Math.min(5.0, rating));
    }
}
