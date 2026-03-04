package com.teamup.server.modules.mentor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.mentor.entity.MentorPerformance;
import com.teamup.server.modules.mentor.mapper.MentorPerformanceMapper;
import com.teamup.server.modules.mentor.mapper.MentorReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 导师评分计算服务
 * 
 * 评分算法说明（优化版）：
 * - 客观数据（60%权重）：
 *   - 成功率（24%）：成功培养学员数 / 总学员数
 *   - 平均信誉分（18%）：学员平均信誉分 / 20（转换为0-5范围）
 *   - 活跃度（18%）：活跃学员数 / 总学员数
 * - 学员评价（40%权重）：
 *   - 学员评价平均分（40%）：来自mentor_reviews表
 * 
 * 最终评分范围：0.00 - 5.00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentorRatingService {
    
    private final MentorPerformanceMapper performanceMapper;
    private final MentorReviewMapper reviewMapper;
    
    /**
     * 计算并更新单个导师的评分
     * 
     * @param mentorId 导师ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMentorRating(Long mentorId) {
        MentorPerformance performance = performanceMapper.selectOne(
            new LambdaQueryWrapper<MentorPerformance>()
                .eq(MentorPerformance::getMentorId, mentorId)
        );
        
        if (performance == null) {
            log.warn("导师绩效数据不存在: mentorId={}", mentorId);
            return;
        }
        
        // 计算评分
        BigDecimal rating = calculateRating(performance);
        
        // 更新评分
        performance.setRating(rating);
        performanceMapper.updateById(performance);
        
        log.info("导师评分已更新: mentorId={}, rating={}", mentorId, rating);
    }
    
    /**
     * 批量更新所有导师的评分
     * 
     * @return 更新的导师数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateAllMentorRatings() {
        log.info("开始更新所有导师评分...");
        
        try {
            List<MentorPerformance> allPerformances = performanceMapper.selectList(null);
            log.info("查询到 {} 条导师绩效记录", allPerformances.size());
            
            int updatedCount = 0;
            for (MentorPerformance performance : allPerformances) {
                try {
                    log.debug("正在更新导师 {} 的评分", performance.getMentorId());
                    BigDecimal rating = calculateRating(performance);
                    performance.setRating(rating);
                    performanceMapper.updateById(performance);
                    updatedCount++;
                    log.debug("导师 {} 评分更新成功: {}", performance.getMentorId(), rating);
                } catch (Exception e) {
                    log.error("更新导师 {} 评分失败", performance.getMentorId(), e);
                    throw e; // 重新抛出异常以触发事务回滚
                }
            }
            
            log.info("已更新所有导师评分，共 {} 位导师", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            log.error("更新所有导师评分失败", e);
            throw e;
        }
    }
    
    /**
     * 计算导师评分（优化算法，整合学员评价）
     * 
     * 算法说明：
     * 1. 客观数据（60%权重）：
     *    - 成功率（24%）= 成功培养学员数 / 总学员数
     *    - 平均信誉分（18%）= 学员平均信誉分 / 20（转换为0-5范围）
     *    - 活跃度（18%）= 活跃学员数 / 总学员数
     * 2. 学员评价（40%权重）：
     *    - 学员评价平均分（40%）= 来自mentor_reviews表
     * 
     * 综合评分 = (客观评分 × 0.6 + 主观评分 × 0.4) × 5.0
     * 
     * @param performance 导师绩效数据
     * @return 评分（0.00 - 5.00）
     */
    private BigDecimal calculateRating(MentorPerformance performance) {
        // === 第一部分：客观数据（60%权重）===
        
        // 1. 成功率（24%）
        double successRate = 0;
        if (performance.getTotalMentees() != null && performance.getTotalMentees() > 0) {
            int successful = performance.getSuccessfulMentees() != null ? performance.getSuccessfulMentees() : 0;
            successRate = (double) successful / performance.getTotalMentees();
        }
        
        // 2. 平均信誉分（18%）- 转换为0-1范围
        double avgScore = 0;
        if (performance.getAverageMenteeScore() != null) {
            avgScore = performance.getAverageMenteeScore().doubleValue() / 100.0;
        }
        
        // 3. 活跃度（18%）
        double activeRate = 0;
        if (performance.getTotalMentees() != null && performance.getTotalMentees() > 0) {
            int active = performance.getActiveMentees() != null ? performance.getActiveMentees() : 0;
            activeRate = (double) active / performance.getTotalMentees();
        }
        
        // 客观评分（0-1范围）
        double objectiveScore = successRate * 0.4 + avgScore * 0.3 + activeRate * 0.3;
        
        // === 第二部分：学员评价（40%权重）===
        
        // 获取学员评价平均分（1-5分）
        BigDecimal avgReview = reviewMapper.getAverageRating(performance.getMentorId());
        double subjectiveScore = 0.5; // 默认中等分数
        
        if (avgReview != null && avgReview.compareTo(BigDecimal.ZERO) > 0) {
            // 转换为0-1范围
            subjectiveScore = avgReview.doubleValue() / 5.0;
            log.debug("导师 {} 的学员评价平均分: {}", performance.getMentorId(), avgReview);
        } else {
            log.debug("导师 {} 暂无学员评价，使用默认分数", performance.getMentorId());
        }
        
        // === 综合计算 ===
        
        // 综合评分 = 客观评分(60%) + 主观评分(40%)
        double finalScore = (objectiveScore * 0.6 + subjectiveScore * 0.4);
        
        // 转换为0-5范围
        double rating = finalScore * 5.0;
        
        // 确保在0-5范围内
        rating = Math.max(0, Math.min(5.0, rating));
        
        log.debug("导师 {} 评分计算: 客观={}, 主观={}, 最终={}", 
                 performance.getMentorId(), objectiveScore, subjectiveScore, rating);
        
        // 保留两位小数
        return BigDecimal.valueOf(rating).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 获取导师当前评分（不更新）
     * 
     * @param mentorId 导师ID
     * @return 评分，如果导师不存在返回null
     */
    public BigDecimal getMentorRating(Long mentorId) {
        MentorPerformance performance = performanceMapper.selectOne(
            new LambdaQueryWrapper<MentorPerformance>()
                .eq(MentorPerformance::getMentorId, mentorId)
        );
        
        return performance != null ? performance.getRating() : null;
    }
}
