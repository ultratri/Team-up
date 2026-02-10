package com.teamup.server.modules.competition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.audit.AuditLogService;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.competition.entity.TeamCompetitionScore;
import com.teamup.server.modules.competition.service.TeamCompetitionScoreService;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.competition.mapper.TeamCompetitionScoreMapper;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
public class CompetitionScoreController {

    private final TeamCompetitionScoreService scoreService;
    private final TeamService teamService;
    private final AuditLogService auditLogService;
    private final TeamCompetitionScoreMapper teamCompetitionScoreMapper;

    /**
     * 给队伍打分（导师/管理员）
     * body: { teamId, score, comment }
     */
    @PostMapping("/{competitionId}/scores")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<TeamCompetitionScore> upsertScore(
            @PathVariable Long competitionId,
            @RequestBody Map<String, Object> body
    ) {
        Long teamId = body.get("teamId") != null ? Long.valueOf(String.valueOf(body.get("teamId"))) : null;
        if (teamId == null) return Result.error(400, "teamId 不能为空");

        BigDecimal score = body.get("score") != null ? new BigDecimal(String.valueOf(body.get("score"))) : null;
        if (score == null) return Result.error(400, "score 不能为空");

        String comment = body.get("comment") != null ? String.valueOf(body.get("comment")) : null;

        Team team = teamService.getById(teamId);
        if (team == null || team.getCompetitionId() == null || !competitionId.equals(team.getCompetitionId())) {
            return Result.error(400, "队伍不属于该比赛");
        }

        Long userId = UserContext.getCurrentUserId();

        // 若当前用户是导师，则只允许给自己指导的队伍打分
        // 管理员角色由 @PreAuthorize 控制，这里主要限制 mentor 身份
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isMentor = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch(a -> "ROLE_MENTOR".equals(a));
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch(a -> "ROLE_PLATFORM_ADMIN".equals(a));
            if (isMentor && !isAdmin) {
                if (team.getMentorId() == null || !userId.equals(team.getMentorId())) {
                    return Result.error(403, "只能给自己指导的队伍打分");
                }
            }
        }

        TeamCompetitionScore existing = scoreService.getOne(new LambdaQueryWrapper<TeamCompetitionScore>()
                .eq(TeamCompetitionScore::getCompetitionId, competitionId)
                .eq(TeamCompetitionScore::getTeamId, teamId));

        if (existing == null) {
            TeamCompetitionScore s = new TeamCompetitionScore();
            s.setCompetitionId(competitionId);
            s.setTeamId(teamId);
            s.setScore(score);
            s.setComment(comment);
            s.setScoredBy(userId);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            scoreService.save(s);
            auditLogService.logSensitiveOperation("SCORE_COMPETITION_TEAM", "COMPETITION", competitionId,
                    "给队伍打分 teamId=" + teamId + ", score=" + score, "SUCCESS", null);
            return Result.success(s);
        }

        existing.setScore(score);
        existing.setComment(comment);
        existing.setScoredBy(userId);
        existing.setUpdatedAt(LocalDateTime.now());
        scoreService.updateById(existing);
        auditLogService.logSensitiveOperation("SCORE_COMPETITION_TEAM", "COMPETITION", competitionId,
                "更新队伍评分 teamId=" + teamId + ", score=" + score, "SUCCESS", null);
        return Result.success(existing);
    }

    /**
     * 获取所有已评分的队伍引用（competitionId, teamId），用于前端筛选已评分/未评分
     */
    @GetMapping("/scores/scored-teams")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','DEPT_ADMIN','MENTOR')")
    public Result<java.util.List<java.util.Map<String, Object>>> listScoredTeams() {
        java.util.List<java.util.Map<String, Object>> rows = teamCompetitionScoreMapper.selectAllScoredTeams();
        return Result.success(rows);
    }
}

