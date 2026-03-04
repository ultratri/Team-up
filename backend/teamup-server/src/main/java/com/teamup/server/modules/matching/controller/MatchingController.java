package com.teamup.server.modules.matching.controller;

import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.project.dto.matching.MatchResult;
import com.teamup.server.modules.project.service.MatchingIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 匹配相关通用接口（智能组队推荐等）
 */
@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingIntegrationService matchingIntegrationService;

    /**
     * 智能组队推荐：为当前用户推荐可组队的同学
     * 
     * @deprecated 独立的智能组队推荐页面已移除，此接口保留供未来在项目流程中使用
     * 未来用途：
     * 1. 在项目详情页推荐适合一起申请的队友
     * 2. 在创建项目时推荐适合邀请的队友
     */
    @Deprecated
    @GetMapping("/recommend-teammates")
    public Result<List<MatchResult>> recommendTeammates(
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = SecurityUtils.getUserId();
        List<MatchResult> results = matchingIntegrationService.recommendTeammates(userId, limit);
        return Result.success(results);
    }

    /**
     * 为项目推荐队友：基于项目需求推荐合适的队友
     * 用于创建项目时根据项目需求推荐合适的队友
     */
    @PostMapping("/recommend-teammates-for-project")
    public Result<Map<String, Object>> recommendTeammatesForProject(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getUserId();
        
        String projectTitle = (String) request.get("projectTitle");
        String projectType = (String) request.get("projectType");
        Integer weeklyHours = request.get("weeklyHours") != null ? 
                ((Number) request.get("weeklyHours")).intValue() : null;
        Integer expectedDuration = request.get("expectedDuration") != null ? 
                ((Number) request.get("expectedDuration")).intValue() : null;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skillRequirements = (List<Map<String, Object>>) request.get("skillRequirements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> timeSlots = (List<Map<String, Object>>) request.get("timeSlots");
        
        List<MatchResult> results = matchingIntegrationService.recommendTeammatesForProject(
                userId, projectTitle, projectType, skillRequirements, timeSlots, weeklyHours, expectedDuration
        );
        
        return Result.success(Map.of("recommendations", results));
    }
}

