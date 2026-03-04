package com.teamup.server.modules.user.controller;

import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.user.dto.ExperienceScore;
import com.teamup.server.modules.user.entity.UserProjectHistory;
import com.teamup.server.modules.user.service.ProjectHistoryService;
import com.teamup.server.modules.user.vo.ProjectHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目履历控制器
 */
@Slf4j
@RestController
@RequestMapping("/user/project-history")
@RequiredArgsConstructor
public class ProjectHistoryController {
    
    private final ProjectHistoryService projectHistoryService;
    
    /**
     * 获取当前用户的项目履历
     */
    @GetMapping("/my")
    public Result<List<ProjectHistoryVO>> getMyProjectHistory(
            @RequestParam(defaultValue = "true") boolean onlyCompleted
    ) {
        Long userId = SecurityUtils.getUserId();
        List<UserProjectHistory> history = projectHistoryService.getUserProjectHistory(userId, onlyCompleted);
        
        List<ProjectHistoryVO> voList = history.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return Result.success(voList);
    }
    
    /**
     * 获取指定用户的项目履历（公开信息）
     */
    @GetMapping("/user/{userId}")
    public Result<List<ProjectHistoryVO>> getUserProjectHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "true") boolean onlyCompleted
    ) {
        List<UserProjectHistory> history = projectHistoryService.getUserProjectHistory(userId, onlyCompleted);
        
        List<ProjectHistoryVO> voList = history.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return Result.success(voList);
    }
    
    /**
     * 获取当前用户的经验分数
     */
    @GetMapping("/my/experience-score")
    public Result<ExperienceScore> getMyExperienceScore() {
        Long userId = SecurityUtils.getUserId();
        ExperienceScore score = projectHistoryService.calculateExperienceScore(userId);
        return Result.success(score);
    }
    
    /**
     * 获取指定用户的经验分数
     */
    @GetMapping("/user/{userId}/experience-score")
    public Result<ExperienceScore> getUserExperienceScore(@PathVariable Long userId) {
        ExperienceScore score = projectHistoryService.calculateExperienceScore(userId);
        return Result.success(score);
    }
    
    /**
     * 手动触发同步（管理员功能）
     */
    @PostMapping("/sync")
    public Result<Void> syncProjectHistory() {
        // TODO: 添加管理员权限检查
        projectHistoryService.syncAllProjectHistory();
        return Result.success(null);
    }
    
    /**
     * 转换为VO
     */
    private ProjectHistoryVO convertToVO(UserProjectHistory history) {
        ProjectHistoryVO vo = new ProjectHistoryVO();
        BeanUtils.copyProperties(history, vo);
        
        // 计算综合评分
        if (history.getEvaluationCount() != null && history.getEvaluationCount() > 0) {
            double tech = history.getAvgTechScore() != null 
                ? history.getAvgTechScore().doubleValue() : 0.0;
            double collab = history.getAvgCollaborationScore() != null 
                ? history.getAvgCollaborationScore().doubleValue() : 0.0;
            double task = history.getAvgTaskCompletionScore() != null 
                ? history.getAvgTaskCompletionScore().doubleValue() : 0.0;
            
            double avgScore = tech * 0.4 + collab * 0.3 + task * 0.3;
            vo.setAvgScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
        }
        
        return vo;
    }
}
