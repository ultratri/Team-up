package com.teamup.server.modules.project.controller;

import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.notification.service.NotificationService;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.service.MatchingIntegrationService;
import com.teamup.server.modules.project.service.ProjectService;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 项目邀请控制器
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectInvitationController {

    private final ProjectService projectService;
    private final MatchingIntegrationService matchingIntegrationService;
    private final NotificationService notificationService;
    private final TeamService teamService;
    private final UserService userService;

    @PostMapping("/{projectId}/invite/{userId}")
    public Result<Void> inviteCandidate(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        Long inviterId = SecurityUtils.getUserId();

        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            return Result.error(404, "项目不存在");
        }

        if (!project.getCreatorId().equals(inviterId)) {
            return Result.error(403, "只有项目创建者可以邀请成员");
        }

        if (!"RECRUITING".equals(project.getStatus())) {
            return Result.error(400, "只有招募中的项目可以邀请成员");
        }

        User invitedUser = userService.getUserById(userId);
        if (invitedUser == null) {
            return Result.error(404, "用户不存在");
        }

        String message = payload != null ? (String) payload.get("message") : null;
        if (message == null || message.trim().isEmpty()) {
            message = "邀请你加入项目";
        }

        @SuppressWarnings("unchecked")
        Map<String, Double> breakdown = payload != null ? (Map<String, Double>) payload.get("breakdown") : null;

        try {
            notificationService.createNotification(
                    userId,
                    "PROJECT_INVITATION",
                    "收到项目邀请",
                    String.format("项目「%s」邀请你加入。%s", project.getTitle(), message),
                    "PROJECT",
                    projectId
            );

            matchingIntegrationService.reportMatchFeedback(
                    projectId,
                    userId,
                    project.getProjectType(),
                    "INVITED",
                    "PROJECT_INVITATION_CONTROLLER",
                    System.currentTimeMillis(),
                    breakdown
            );

            log.info("项目邀请已发送: projectId={}, inviterId={}, invitedUserId={}, hasBreakdown={}",
                    projectId, inviterId, userId, breakdown != null && !breakdown.isEmpty());

            return Result.success(null, "邀请已发送");
        } catch (Exception e) {
            log.error("发送项目邀请失败", e);
            return Result.error(500, "发送邀请失败");
        }
    }

    @PostMapping("/{projectId}/invitations/respond")
    public Result<Void> respondInvitation(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> payload
    ) {
        Long currentUserId = SecurityUtils.getUserId();

        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            return Result.error(404, "项目不存在");
        }

        if (payload == null) {
            return Result.error(400, "请求参数不能为空");
        }

        Object invitedUserIdObj = payload.get("userId");
        if (invitedUserIdObj == null) {
            return Result.error(400, "缺少 userId 参数");
        }

        Long invitedUserId;
        try {
            invitedUserId = Long.valueOf(String.valueOf(invitedUserIdObj));
        } catch (Exception e) {
            return Result.error(400, "userId 参数格式错误");
        }

        if (!currentUserId.equals(invitedUserId)) {
            return Result.error(403, "只能响应自己的邀请");
        }

        String action = payload.get("action") == null ? "" : String.valueOf(payload.get("action")).trim().toUpperCase();
        if (!"ACCEPT".equals(action) && !"DECLINE".equals(action)) {
            return Result.error(400, "action 仅支持 ACCEPT 或 DECLINE");
        }

        @SuppressWarnings("unchecked")
        Map<String, Double> breakdown = payload.get("breakdown") instanceof Map
                ? (Map<String, Double>) payload.get("breakdown")
                : new HashMap<>();

        try {
            if ("ACCEPT".equals(action)) {
                if (project.getTeamId() == null) {
                    return Result.error(400, "项目尚未绑定团队，无法接受邀请");
                }
                teamService.addMember(project.getTeamId(), invitedUserId);

                matchingIntegrationService.reportMatchFeedback(
                        projectId,
                        invitedUserId,
                        project.getProjectType(),
                        "INVITE_ACCEPTED",
                        "PROJECT_INVITATION_CONTROLLER",
                        System.currentTimeMillis(),
                        breakdown
                );
            } else {
                matchingIntegrationService.reportMatchFeedback(
                        projectId,
                        invitedUserId,
                        project.getProjectType(),
                        "INVITE_DECLINED",
                        "PROJECT_INVITATION_CONTROLLER",
                        System.currentTimeMillis(),
                        breakdown
                );
            }

            log.info("项目邀请响应成功: projectId={}, userId={}, action={}, hasBreakdown={}",
                    projectId, invitedUserId, action, breakdown != null && !breakdown.isEmpty());
            return Result.success(null, "邀请响应成功");
        } catch (RuntimeException e) {
            log.warn("项目邀请响应失败: projectId={}, userId={}, action={}, message={}",
                    projectId, invitedUserId, action, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("项目邀请响应异常", e);
            return Result.error(500, "邀请响应失败");
        }
    }
}
