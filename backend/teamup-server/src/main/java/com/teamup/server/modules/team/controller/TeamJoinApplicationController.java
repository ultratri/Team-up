package com.teamup.server.modules.team.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamJoinApplication;
import com.teamup.server.modules.team.service.TeamJoinApplicationService;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 队伍加入申请接口
 * 
 * 注意：此控制器不使用 @PreAuthorize 注解，而是在方法内部通过 UserContext 获取当前用户
 * 这样可以避免 Spring Security 的复杂配置问题
 */
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class TeamJoinApplicationController {

    private final TeamJoinApplicationService joinApplicationService;
    private final TeamService teamService;

    /**
     * 在比赛详情页申请加入队伍
     */
    @PostMapping("/competitions/{competitionId}/teams/{teamId}/join")
    public Result<TeamJoinApplication> applyJoinCompetitionTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Team team = teamService.getById(teamId);
            if (team == null) {
                return Result.error(404, "队伍不存在");
            }
            if (team.getCompetitionId() == null || !competitionId.equals(team.getCompetitionId())) {
                return Result.error(400, "该队伍不属于该比赛");
            }
            String reason = payload != null ? (String) payload.get("reason") : null;
            TeamJoinApplication app = joinApplicationService.apply(teamId, userId, reason);
            return Result.success(app);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("未登录")) {
                return Result.error(401, "请先登录");
            }
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 队长查看某个队伍的加入申请
     */
    @GetMapping("/teams/{teamId}/join-applications")
    public Result<Page<TeamJoinApplication>> listForTeam(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Page<TeamJoinApplication> result = joinApplicationService.listForTeam(teamId, userId, page, size, status);
            return Result.success(result);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("未登录")) {
                return Result.error(401, "请先登录");
            }
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 我的加入申请列表
     */
    @GetMapping("/teams/join-applications/my")
    public Result<Page<TeamJoinApplication>> listMy(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Page<TeamJoinApplication> result = joinApplicationService.listMy(userId, page, size, status);
            return Result.success(result);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("未登录")) {
                return Result.error(401, "请先登录");
            }
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 队长审核加入申请
     */
    @PostMapping("/teams/join-applications/{id}/review")
    public Result<Void> review(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comment
    ) {
        try {
            Long userId = UserContext.getCurrentUserId();
            joinApplicationService.review(id, userId, approved, comment);
            return Result.success();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("未登录")) {
                return Result.error(401, "请先登录");
            }
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 撤回申请（仅申请人可撤回，且仅限 PENDING）
     */
    @PostMapping("/teams/join-applications/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        try {
            Long userId = UserContext.getCurrentUserId();
            joinApplicationService.withdraw(id, userId);
            return Result.success();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("未登录")) {
                return Result.error(401, "请先登录");
            }
            return Result.error(500, e.getMessage());
        }
    }
}
