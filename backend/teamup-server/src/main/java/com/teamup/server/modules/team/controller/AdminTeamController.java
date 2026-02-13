package com.teamup.server.modules.team.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.dto.DissolveTeamRequest;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.team.vo.AdminTeamDetailVO;
import com.teamup.server.modules.team.vo.AdminTeamListVO;
import com.teamup.server.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员团队管理接口
 */
@RestController
@RequestMapping("/admin/teams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminTeamController {

    private final TeamService teamService;
    private final NotificationService notificationService;

    /**
     * 获取团队列表（分页）
     */
    @GetMapping
    public Result<Page<AdminTeamListVO>> listTeams(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword
    ) {
        Page<AdminTeamListVO> result = teamService.getAdminTeamList(page, size, type, isActive, keyword);
        return Result.success(result);
    }

    /**
     * 获取团队详情（管理员视图）
     */
    @GetMapping("/{id}")
    public Result<AdminTeamDetailVO> getTeamDetail(@PathVariable Long id) {
        AdminTeamDetailVO detail = teamService.getAdminTeamDetail(id);
        if (detail == null) {
            return Result.error(404, "团队不存在");
        }
        return Result.success(detail);
    }

    /**
     * 解散团队
     */
    @PostMapping("/{id}/dissolve")
    public Result<Void> dissolveTeam(
            @PathVariable Long id,
            @RequestBody DissolveTeamRequest request
    ) {
        Team team = teamService.getTeamById(id);
        if (team == null) {
            return Result.error(404, "团队不存在");
        }

        // 通知团队成员
        List<TeamMember> members = teamService.getTeamMembersByTeamId(id);
        for (TeamMember member : members) {
            notificationService.createNotification(
                member.getUserId(),
                "SYSTEM",
                "团队已被管理员解散",
                String.format("您所在的团队\"%s\"已被管理员解散。原因：%s", 
                    team.getTeamName(), request.getReason()),
                "TEAM",
                id
            );
        }
        
        // 解散团队（删除所有成员）
        for (TeamMember member : members) {
            teamService.removeMember(id, member.getUserId());
        }

        return Result.success();
    }

    /**
     * 移除团队成员
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    public Result<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @RequestParam(required = false) String reason
    ) {
        Team team = teamService.getTeamById(teamId);
        if (team == null) {
            return Result.error(404, "团队不存在");
        }

        // 移除成员
        teamService.removeMember(teamId, userId);

        // 通知被移除的用户
        notificationService.createNotification(
            userId,
            "SYSTEM",
            "您已被移出团队",
            String.format("您已被管理员从团队\"%s\"中移除。%s", 
                team.getTeamName(), 
                reason != null ? "原因：" + reason : ""),
            "TEAM",
            teamId
        );

        return Result.success();
    }

    /**
     * 批量解散团队
     */
    @PostMapping("/batch-dissolve")
    public Result<Void> batchDissolveTeams(@RequestBody List<Long> teamIds) {
        for (Long teamId : teamIds) {
            Team team = teamService.getTeamById(teamId);
            if (team != null) {
                // 通知团队成员
                List<TeamMember> members = teamService.getTeamMembersByTeamId(teamId);
                for (TeamMember member : members) {
                    notificationService.createNotification(
                        member.getUserId(),
                        "SYSTEM",
                        "团队已被管理员解散",
                        String.format("您所在的团队\"%s\"已被管理员批量解散。", team.getTeamName()),
                        "TEAM",
                        teamId
                    );
                }
                
                // 解散团队（删除所有成员）
                for (TeamMember member : members) {
                    teamService.removeMember(teamId, member.getUserId());
                }
            }
        }
        return Result.success();
    }

    /**
     * 获取团队统计信息
     */
    @GetMapping("/statistics")
    public Result<Object> getTeamStatistics() {
        return Result.success(teamService.getAdminTeamStatistics());
    }
}
