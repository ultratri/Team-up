package com.teamup.server.modules.evaluation.controller;

import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.evaluation.dto.EvaluationDTO;
import com.teamup.server.modules.evaluation.service.EvaluationService;
import com.teamup.server.modules.evaluation.vo.EvaluationVO;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final TeamService teamService;
    private final TeamMemberMapper teamMemberMapper;

    /**
     * 提交评价
     * @param teamId 团队ID
     * @param evaluationDTO 评价数据
     * @return 成功响应
     */
    @PostMapping("/{teamId}/evaluations")
    public Result<Void> submitEvaluation(
            @PathVariable Long teamId,
            @RequestBody @Valid EvaluationDTO evaluationDTO) {
        
        try {
            // 验证团队是否存在
            Team team = teamService.getTeamById(teamId);
            if (team == null) {
                return Result.error(404, "团队不存在");
            }
            
            // 获取当前用户ID
            Long currentUserId = UserContext.getCurrentUserId();
            
            // 验证用户是否为团队成员
            if (!isTeamMember(teamId, currentUserId)) {
                return Result.error(403, "无权限访问该团队");
            }
            
            // 验证被评价者是否为团队成员
            if (!isTeamMember(teamId, evaluationDTO.getEvaluatedId())) {
                return Result.error(400, "被评价者不是该团队成员");
            }
            
            // 验证是否为自我评价
            if (currentUserId.equals(evaluationDTO.getEvaluatedId())) {
                return Result.error(400, "不能评价自己");
            }
            
            // 提交评价
            evaluationService.submitEvaluation(teamId, currentUserId, evaluationDTO);
            
            return Result.success();
            
        } catch (BusinessException e) {
            // 处理业务异常（如重复评价）
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            // 处理其他异常
            return Result.error(500, "提交评价失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取团队评价列表
     * @param teamId 团队ID
     * @return 评价列表
     */
    @GetMapping("/{teamId}/evaluations")
    public Result<List<EvaluationVO>> getTeamEvaluations(@PathVariable Long teamId) {
        try {
            // 验证团队是否存在
            Team team = teamService.getTeamById(teamId);
            if (team == null) {
                return Result.error(404, "团队不存在");
            }
            
            // 获取当前用户ID
            Long currentUserId = UserContext.getCurrentUserId();
            
            // 验证用户是否为团队成员
            if (!isTeamMember(teamId, currentUserId)) {
                return Result.error(403, "无权限访问该团队数据");
            }
            
            // 获取评价列表
            List<EvaluationVO> evaluations = evaluationService.getEvaluations(teamId);
            return Result.success(evaluations);
            
        } catch (Exception e) {
            return Result.error(500, "获取评价列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查用户是否为团队成员
     */
    private boolean isTeamMember(Long teamId, Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("user_id", userId);
        return teamMemberMapper.selectCount(wrapper) > 0;
    }
}
