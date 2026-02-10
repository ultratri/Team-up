package com.teamup.server.modules.team.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamJoinApplication;
import com.teamup.server.modules.team.service.TeamJoinApplicationService;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 队伍加入申请接口
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class TeamJoinApplicationController {

    private final TeamJoinApplicationService joinApplicationService;
    private final TeamService teamService;

    /**
     * 在比赛详情页申请加入队伍
     */
    @PostMapping("/api/competitions/{competitionId}/teams/{teamId}/join")
    @PreAuthorize("isAuthenticated()")
    public Result<TeamJoinApplication> applyJoinCompetitionTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
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
    }

    /**
     * 队长查看某个队伍的加入申请
     */
    @GetMapping("/api/teams/{teamId}/join-applications")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<TeamJoinApplication>> listForTeam(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Long userId = UserContext.getCurrentUserId();
        Page<TeamJoinApplication> result = joinApplicationService.listForTeam(teamId, userId, page, size, status);
        return Result.success(result);
    }

    /**
     * 我的加入申请列表
     */
    @GetMapping("/api/teams/join-applications/my")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<TeamJoinApplication>> listMy(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Long userId = UserContext.getCurrentUserId();
        Page<TeamJoinApplication> result = joinApplicationService.listMy(userId, page, size, status);
        return Result.success(result);
    }

    /**
     * 队长审核加入申请
     */
    @PostMapping("/api/teams/join-applications/{id}/review")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> review(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comment
    ) {
        Long userId = UserContext.getCurrentUserId();
        joinApplicationService.review(id, userId, approved, comment);
        return Result.success();
    }

    /**
     * 撤回申请（仅申请人可撤回，且仅限 PENDING）
     */
    @PostMapping("/api/teams/join-applications/{id}/withdraw")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> withdraw(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        joinApplicationService.withdraw(id, userId);
        return Result.success();
    }
}

