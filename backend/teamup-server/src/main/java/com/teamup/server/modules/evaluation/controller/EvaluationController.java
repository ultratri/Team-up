package com.teamup.server.modules.evaluation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.evaluation.entity.Evaluation;
import com.teamup.server.modules.user.entity.CollaborationHistory;
import com.teamup.server.modules.evaluation.mapper.EvaluationMapper;
import com.teamup.server.modules.user.mapper.CollaborationHistoryMapper;
import com.teamup.server.modules.user.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 项目评价控制器
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {
    
    private final EvaluationMapper evaluationMapper;
    private final CollaborationHistoryMapper collaborationHistoryMapper;
    private final CreditService creditService;
    
    /**
     * 批量提交项目成员评价
     */
    @PostMapping("/batch")
    @Transactional
    public Result<Void> batchEvaluate(@RequestBody Map<String, Object> request) {
        Long evaluatorId = SecurityUtils.getUserId();
        Long projectId = ((Number) request.get("projectId")).longValue();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evaluations = (List<Map<String, Object>>) request.get("evaluations");
        
        if (evaluations == null || evaluations.isEmpty()) {
            return Result.error(400, "评价列表不能为空");
        }
        
        for (Map<String, Object> evalData : evaluations) {
            Long evaluateeId = ((Number) evalData.get("evaluateeId")).longValue();
            Integer techContribution = ((Number) evalData.get("techContribution")).intValue();
            Integer collaboration = ((Number) evalData.get("collaboration")).intValue();
            Integer taskCompletion = ((Number) evalData.get("taskCompletion")).intValue();
            String comment = (String) evalData.get("comment");
            Boolean anonymous = (Boolean) evalData.getOrDefault("anonymous", false);
            
            // 1. 保存评价记录
            Evaluation evaluation = new Evaluation();
            evaluation.setProjectId(projectId);
            evaluation.setEvaluatorId(evaluatorId);
            evaluation.setEvaluatedId(evaluateeId);
            evaluation.setTechContributionScore(techContribution);
            evaluation.setCollaborationScore(collaboration);
            evaluation.setTaskCompletionScore(taskCompletion);
            evaluation.setComment(comment);
            evaluation.setIsAnonymous(anonymous);
            evaluation.setCreatedAt(LocalDateTime.now());
            evaluationMapper.insert(evaluation);
            
            // 2. 计算协作得分(0.0-1.0)
            double score = (techContribution + collaboration + taskCompletion) / 3.0 / 5.0;
            
            // 3. 生成协作历史记录
            CollaborationHistory history = new CollaborationHistory();
            history.setUserId(evaluateeId);
            history.setPartnerId(evaluatorId);
            history.setProjectId(projectId);
            history.setCollaborationScore(java.math.BigDecimal.valueOf(score));
            history.setCreatedAt(LocalDateTime.now());
            collaborationHistoryMapper.insert(history);
            
            // 4. 触发信誉分变更
            int totalScore = techContribution + collaboration + taskCompletion;
            double avgScore = totalScore / 3.0;
            
            if (avgScore >= 4.0) {
                // 好评: 平均分≥4
                creditService.addCreditRecord(
                    evaluateeId, 5, 
                    "GOOD_REVIEW", 
                    projectId, 
                    String.format("收到好评(平均%.1f分)", avgScore)
                );
            } else if (avgScore <= 2.0) {
                // 差评: 平均分≤2
                creditService.addCreditRecord(
                    evaluateeId, -10, 
                    "BAD_REVIEW", 
                    projectId, 
                    String.format("收到差评(平均%.1f分)", avgScore)
                );
            }
            
            log.info("项目评价已保存: projectId={}, evaluator={}, evaluatee={}, score={}", 
                projectId, evaluatorId, evaluateeId, score);
        }
        
        return Result.success(null, "评价提交成功");
    }
    
    /**
     * 获取项目的评价列表
     */
    @GetMapping("/project/{projectId}")
    public Result<List<Evaluation>> getProjectEvaluations(@PathVariable Long projectId) {
        List<Evaluation> evaluations = evaluationMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getProjectId, projectId)
                .orderByDesc(Evaluation::getCreatedAt)
        );
        return Result.success(evaluations);
    }
    
    /**
     * 获取用户收到的评价
     */
    @GetMapping("/user/{userId}")
    public Result<List<Evaluation>> getUserEvaluations(@PathVariable Long userId) {
        List<Evaluation> evaluations = evaluationMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluatedId, userId)
                .orderByDesc(Evaluation::getCreatedAt)
        );
        return Result.success(evaluations);
    }
}
