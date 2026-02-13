package com.teamup.server.modules.competition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导师端：我指导的比赛队伍（用于评分/查看）
 */
@RestController
@RequestMapping("/mentor")
@RequiredArgsConstructor
public class MentorScoringController {

    private final TeamService teamService;

    @GetMapping("/competition-teams")
    @PreAuthorize("hasAnyRole('MENTOR','PLATFORM_ADMIN')")
    public Result<Page<Team>> listMyCompetitionTeams(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String keyword
    ) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;

        Long mentorId = UserContext.getCurrentUserId();
        Page<Team> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Team::getMentorId, mentorId);
        wrapper.eq(Team::getTeamNature, "LONG_TERM"); // COMPETITION对应LONG_TERM
        if (competitionId != null) {
            wrapper.eq(Team::getCompetitionId, competitionId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Team::getTeamName, keyword);
        }
        wrapper.orderByDesc(Team::getUpdatedAt);
        return Result.success(teamService.page(pageParam, wrapper));
    }
}

