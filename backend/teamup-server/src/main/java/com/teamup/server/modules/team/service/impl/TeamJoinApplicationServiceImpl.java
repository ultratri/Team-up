package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.notification.service.NotificationService;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamJoinApplication;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamJoinApplicationMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamJoinApplicationService;
import com.teamup.server.modules.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.service.CompetitionService;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TeamJoinApplicationServiceImpl extends ServiceImpl<TeamJoinApplicationMapper, TeamJoinApplication>
        implements TeamJoinApplicationService {

    private static final String STATUS_PENDING = "PENDING";

    private final TeamService teamService;
    private final TeamMemberMapper teamMemberMapper;
    private final NotificationService notificationService;
    private final CompetitionService competitionService;
    private final ProfileService profileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamJoinApplication apply(Long teamId, Long applicantId, String reason) {
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        // 若比赛设置了 maxTeamsPerUser / 报名资格，则按约束限制
        if (team.getCompetitionId() != null) {
            Competition competition = competitionService.getById(team.getCompetitionId());
            if (competition != null) {
                if (competition.getMaxTeamsPerUser() != null && competition.getMaxTeamsPerUser() > 0) {
                    Long cnt = teamMemberMapper.countUserMembershipInCompetition(applicantId, team.getCompetitionId());
                    if (cnt != null && cnt >= competition.getMaxTeamsPerUser()) {
                        throw new RuntimeException("该比赛每人最多可参加 " + competition.getMaxTeamsPerUser() + " 支队伍");
                    }
                }
                if (Boolean.TRUE.equals(competition.getEligibilityEnabled())) {
                    UserProfile profile = profileService.getProfileByUserId(applicantId);
                    if (!checkCompetitionAudienceEligibility(competition, profile)) {
                        throw new RuntimeException("当前比赛限定报名对象，不符合条件的同学无法申请加入队伍");
                    }
                }
            }
        }

        // 已是成员则不能申请
        TeamMember member = teamMemberMapper.selectByTeamAndUser(teamId, applicantId);
        if (member != null) {
            throw new RuntimeException("你已经是该队伍成员");
        }

        // 检查是否已有未处理申请
        LambdaQueryWrapper<TeamJoinApplication> exists = new LambdaQueryWrapper<>();
        exists.eq(TeamJoinApplication::getTeamId, teamId)
                .eq(TeamJoinApplication::getApplicantId, applicantId)
                .in(TeamJoinApplication::getStatus, "PENDING", "APPROVED");
        if (count(exists) > 0) {
            throw new RuntimeException("已提交申请或已加入该队伍");
        }

        TeamJoinApplication app = new TeamJoinApplication();
        app.setTeamId(teamId);
        app.setCompetitionId(team.getCompetitionId());
        app.setApplicantId(applicantId);
        app.setReason(reason);
        app.setStatus(STATUS_PENDING);
        app.setAppliedAt(LocalDateTime.now());
        save(app);

        // 通知队长
        try {
            Long leaderId = team.getLeaderId();
            if (leaderId != null) {
                String teamName = team.getTeamName() != null ? team.getTeamName() : ("队伍#" + teamId);
                notificationService.createNotification(
                        leaderId,
                        "TEAM_JOIN_APPLICATION",
                        "新的入队申请：" + teamName,
                        StringUtils.hasText(reason) ? reason : "请前往团队概览查看并处理",
                        "TEAM",
                        teamId
                );
            }
        } catch (Exception ignored) {
        }

        return app;
    }

    /**
     * 根据 competition.audience JSON 与用户档案信息判断是否符合报名资格
     */
    @SuppressWarnings("unchecked")
    private boolean checkCompetitionAudienceEligibility(Competition competition, UserProfile profile) {
        if (competition == null) return true;
        String audienceJson = competition.getAudience();
        if (audienceJson == null || audienceJson.isBlank()) {
            // 未配置 audience：不限制
            return true;
        }
        if (profile == null) {
            // 无档案信息时，严格起见认定为不符合（也可以改为 true 视业务需要）
            return false;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(audienceJson, java.util.Map.class);
            java.util.List<String> departments = map.get("departments") instanceof java.util.List
                    ? (java.util.List<String>) map.get("departments") : java.util.List.of();
            java.util.List<String> majors = map.get("majors") instanceof java.util.List
                    ? (java.util.List<String>) map.get("majors") : java.util.List.of();
            java.util.List<Integer> grades = map.get("grades") instanceof java.util.List
                    ? (java.util.List<Integer>) map.get("grades") : java.util.List.of();

            boolean deptOk = departments.isEmpty() || (profile.getDepartment() != null && departments.contains(profile.getDepartment()));
            boolean majorOk = majors.isEmpty() || (profile.getMajor() != null && majors.contains(profile.getMajor()));
            boolean gradeOk = grades.isEmpty() || (profile.getGrade() != null && grades.contains(profile.getGrade()));

            return deptOk && majorOk && gradeOk;
        } catch (Exception e) {
            // 配置异常时不阻断报名，视为通过
            return true;
        }
    }

    @Override
    public Page<TeamJoinApplication> listForTeam(Long teamId, Long operatorId, int page, int size, String status) {
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        // 仅队长可查看
        if (!operatorId.equals(team.getLeaderId())) {
            throw new RuntimeException("无权查看该队伍的加入申请");
        }

        Page<TeamJoinApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TeamJoinApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamJoinApplication::getTeamId, teamId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TeamJoinApplication::getStatus, status);
        }
        wrapper.orderByDesc(TeamJoinApplication::getAppliedAt);
        return page(pageParam, wrapper);
    }

    @Override
    public Page<TeamJoinApplication> listMy(Long userId, int page, int size, String status) {
        Page<TeamJoinApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TeamJoinApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamJoinApplication::getApplicantId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TeamJoinApplication::getStatus, status);
        }
        wrapper.orderByDesc(TeamJoinApplication::getAppliedAt);
        return page(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long applicationId, Long operatorId, boolean approved, String comment) {
        TeamJoinApplication app = getById(applicationId);
        if (app == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!STATUS_PENDING.equals(app.getStatus())) {
            throw new RuntimeException("该申请已被处理");
        }

        Team team = teamService.getById(app.getTeamId());
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        if (!operatorId.equals(team.getLeaderId())) {
            throw new RuntimeException("无权审核该申请");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = getBaseMapper().update(
                null,
                new LambdaUpdateWrapper<TeamJoinApplication>()
                        .eq(TeamJoinApplication::getId, applicationId)
                        .eq(TeamJoinApplication::getStatus, STATUS_PENDING)
                        .set(TeamJoinApplication::getStatus, approved ? "APPROVED" : "REJECTED")
                        .set(TeamJoinApplication::getReviewedBy, operatorId)
                        .set(TeamJoinApplication::getReviewComment, comment)
                        .set(TeamJoinApplication::getReviewedAt, now)
        );
        if (updated == 0) {
            throw new RuntimeException("该申请已被处理");
        }

        if (approved) {
            // 再次校验人数上限（避免并发/绕过）
            if (team.getCompetitionId() != null) {
                Competition competition = competitionService.getById(team.getCompetitionId());
                if (competition != null && competition.getMaxTeamsPerUser() != null && competition.getMaxTeamsPerUser() > 0) {
                    Long cnt = teamMemberMapper.countUserMembershipInCompetition(app.getApplicantId(), team.getCompetitionId());
                    if (cnt != null && cnt >= competition.getMaxTeamsPerUser()) {
                        throw new RuntimeException("该比赛每人最多可参加 " + competition.getMaxTeamsPerUser() + " 支队伍");
                    }
                }
            }
            // 加入团队
            teamService.addMember(team.getId(), app.getApplicantId());
        }

        // 通知申请人
        try {
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("队伍#" + team.getId());
            notificationService.createNotification(
                    app.getApplicantId(),
                    approved ? "TEAM_JOIN_APPROVED" : "TEAM_JOIN_REJECTED",
                    approved ? ("入队申请已通过：" + teamName) : ("入队申请被拒绝：" + teamName),
                    StringUtils.hasText(comment) ? comment : (approved ? "你已加入队伍" : "队长拒绝了你的申请"),
                    "TEAM",
                    team.getId()
            );
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long applicationId, Long applicantId) {
        TeamJoinApplication app = getById(applicationId);
        if (app == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!applicantId.equals(app.getApplicantId())) {
            throw new RuntimeException("无权撤回此申请");
        }
        if (!STATUS_PENDING.equals(app.getStatus())) {
            throw new RuntimeException("该申请已被处理，无法撤回");
        }

        int updated = getBaseMapper().update(
                null,
                new LambdaUpdateWrapper<TeamJoinApplication>()
                        .eq(TeamJoinApplication::getId, applicationId)
                        .eq(TeamJoinApplication::getApplicantId, applicantId)
                        .eq(TeamJoinApplication::getStatus, STATUS_PENDING)
                        .set(TeamJoinApplication::getStatus, "WITHDRAWN")
                        .set(TeamJoinApplication::getReviewedAt, LocalDateTime.now())
        );

        if (updated == 0) {
            throw new RuntimeException("该申请已被处理，无法撤回");
        }
    }
}

