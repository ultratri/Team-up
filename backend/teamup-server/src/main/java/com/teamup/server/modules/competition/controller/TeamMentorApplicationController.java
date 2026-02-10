package com.teamup.server.modules.competition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.competition.entity.TeamMentorApplication;
import com.teamup.server.modules.competition.service.TeamMentorApplicationService;
import com.teamup.server.modules.notification.service.NotificationService;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 指导老师申请相关接口
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class TeamMentorApplicationController {

    private final TeamMentorApplicationService applicationService;
    private final TeamService teamService;
    private final NotificationService notificationService;

    /**
     * 队伍发起导师申请
     */
    @PostMapping("/api/teams/{teamId}/mentor-applications")
    @PreAuthorize("isAuthenticated()")
    public Result<TeamMentorApplication> applyMentor(
            @PathVariable Long teamId,
            @RequestParam Long mentorId,
            @RequestParam(required = false) String reason
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        Team team = teamService.getTeamById(teamId);
        if (team == null) {
            return Result.error(404, "团队不存在");
        }

        TeamMentorApplication app = new TeamMentorApplication();
        app.setTeamId(teamId);
        app.setCompetitionId(team.getCompetitionId());
        app.setMentorId(mentorId);
        app.setRequestedBy(currentUserId);
        app.setStatus("PENDING");
        app.setReason(reason);

        applicationService.save(app);

        // 通知目标导师
        try {
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("队伍#" + teamId);
            notificationService.createNotification(
                    mentorId,
                    "MENTOR_APPLICATION",
                    "新的导师申请：" + teamName,
                    (reason != null && !reason.isBlank()) ? reason : "请前往【我的导师申请】查看并处理",
                    "TEAM",
                    teamId
            );
        } catch (Exception ignored) {
        }
        return Result.success(app);
    }

    /**
     * 老师查看自己的导师申请列表
     */
    @GetMapping("/api/mentor/applications")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<TeamMentorApplication>> listForMentor(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Long mentorId = UserContext.getCurrentUserId();
        Page<TeamMentorApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TeamMentorApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMentorApplication::getMentorId, mentorId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TeamMentorApplication::getStatus, status);
        }
        wrapper.orderByDesc(TeamMentorApplication::getCreatedAt);

        Page<TeamMentorApplication> result = applicationService.page(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 老师接受指导申请
     */
    @PostMapping("/api/mentor/applications/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> accept(@PathVariable Long id) {
        Long mentorId = UserContext.getCurrentUserId();
        TeamMentorApplication app = applicationService.getById(id);
        if (app == null) {
            return Result.error(404, "申请不存在");
        }
        if (!mentorId.equals(app.getMentorId())) {
            return Result.error(403, "无权处理该申请");
        }
        app.setStatus("APPROVED");
        app.setDecidedAt(java.time.LocalDateTime.now());
        applicationService.updateById(app);

        // 绑定到队伍
        Team team = teamService.getById(app.getTeamId());
        if (team != null) {
            team.setMentorId(mentorId);
            teamService.updateById(team);
        }

        // 通知申请发起人
        try {
            String teamName = (team != null && team.getTeamName() != null) ? team.getTeamName() : ("队伍#" + app.getTeamId());
            notificationService.createNotification(
                    app.getRequestedBy(),
                    "MENTOR_APPLICATION_APPROVED",
                    "导师申请已通过：" + teamName,
                    "导师已接受你的申请，队伍已绑定指导老师。",
                    "TEAM",
                    app.getTeamId()
            );
        } catch (Exception ignored) {
        }
        return Result.success();
    }

    /**
     * 老师拒绝指导申请
     */
    @PostMapping("/api/mentor/applications/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        Long mentorId = UserContext.getCurrentUserId();
        TeamMentorApplication app = applicationService.getById(id);
        if (app == null) {
            return Result.error(404, "申请不存在");
        }
        if (!mentorId.equals(app.getMentorId())) {
            return Result.error(403, "无权处理该申请");
        }
        app.setStatus("REJECTED");
        if (reason != null) {
            app.setReason(reason);
        }
        app.setDecidedAt(java.time.LocalDateTime.now());
        applicationService.updateById(app);

        // 通知申请发起人
        try {
            Team team = teamService.getById(app.getTeamId());
            String teamName = (team != null && team.getTeamName() != null) ? team.getTeamName() : ("队伍#" + app.getTeamId());
            notificationService.createNotification(
                    app.getRequestedBy(),
                    "MENTOR_APPLICATION_REJECTED",
                    "导师申请被拒绝：" + teamName,
                    (reason != null && !reason.isBlank()) ? reason : "导师已拒绝你的申请",
                    "TEAM",
                    app.getTeamId()
            );
        } catch (Exception ignored) {
        }
        return Result.success();
    }
}

