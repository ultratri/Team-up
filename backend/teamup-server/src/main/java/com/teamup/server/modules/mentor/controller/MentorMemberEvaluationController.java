package com.teamup.server.modules.mentor.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.mentor.dto.MentorMemberEvaluationDTO;
import com.teamup.server.modules.mentor.service.MentorMemberEvaluationService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 导师成员评价控制器
 */
@RestController
@RequestMapping("/mentor/evaluations")
@RequiredArgsConstructor
public class MentorMemberEvaluationController {
    
    private final MentorMemberEvaluationService evaluationService;
    
    /**
     * 获取团队成员列表（供导师评价）
     */
    @GetMapping("/teams/{teamId}/members")
    @PreAuthorize("hasAnyRole('MENTOR', 'PLATFORM_ADMIN')")
    public Result<List<Map<String, Object>>> getTeamMembers(@PathVariable Long teamId) {
        Long mentorId = UserContext.getCurrentUserId();
        List<Map<String, Object>> members = evaluationService.getTeamMembersForEvaluation(teamId, mentorId);
        return Result.success(members);
    }
    
    /**
     * 提交或更新对单个成员的评价
     */
    @PostMapping("/teams/{teamId}/members")
    @PreAuthorize("hasAnyRole('MENTOR', 'PLATFORM_ADMIN')")
    public Result<Void> evaluateMember(
            @PathVariable Long teamId,
            @Validated @RequestBody MentorMemberEvaluationDTO dto) {
        Long mentorId = UserContext.getCurrentUserId();
        evaluationService.evaluateMember(teamId, mentorId, dto);
        return Result.success();
    }
    
    /**
     * 批量提交评价
     */
    @PostMapping("/teams/{teamId}/members/batch")
    @PreAuthorize("hasAnyRole('MENTOR', 'PLATFORM_ADMIN')")
    public Result<Void> batchEvaluateMembers(
            @PathVariable Long teamId,
            @Validated @RequestBody List<MentorMemberEvaluationDTO> evaluations) {
        Long mentorId = UserContext.getCurrentUserId();
        evaluationService.batchEvaluateMembers(teamId, mentorId, evaluations);
        return Result.success();
    }
}
