package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.team.dto.TeamCreateRequest;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.entity.TeamInvitation;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.mapper.TeamInvitationMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.team.vo.AdminTeamDetailVO;
import com.teamup.server.modules.team.vo.AdminTeamListVO;
import com.teamup.server.modules.team.vo.TeamMemberVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.service.CompetitionService;
import com.teamup.server.modules.user.service.UserService;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserInterest;
import com.teamup.server.modules.user.entity.UserAvailability;
import com.teamup.server.modules.user.service.ProfileService;
import com.teamup.server.modules.project.service.ProjectService;
import com.teamup.server.modules.user.mapper.UserInterestMapper;
import com.teamup.server.modules.user.mapper.UserAvailabilityMapper;
import com.teamup.server.modules.tag.mapper.UserTagMapper;
import com.teamup.server.modules.tag.mapper.TagMapper;
import com.teamup.server.modules.tag.entity.UserTag;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.project.client.MatchingFeignClient;
import com.teamup.server.modules.project.dto.matching.UserTeamMatchRequest;
import com.teamup.server.modules.project.dto.matching.UserTeamMatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    private final TeamMemberMapper teamMemberMapper;
    private final TeamInvitationMapper teamInvitationMapper;
    private final UserService userService;
    private final ProfileService profileService;
    private final CompetitionService competitionService;
    @org.springframework.context.annotation.Lazy
    private final ProjectService projectService;
    private final com.teamup.server.modules.notification.service.NotificationService notificationService;
    private final com.teamup.server.modules.user.service.CreditService creditService;
    private final com.teamup.server.modules.team.service.TeamProjectService teamProjectService;
    private final UserInterestMapper userInterestMapper;
    private final UserAvailabilityMapper userAvailabilityMapper;
    private final UserTagMapper userTagMapper;
    private final TagMapper tagMapper;
    private final MatchingFeignClient matchingFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Team createTeam(TeamCreateRequest request) {
        if ("COMPETITION".equals(request.getType()) && request.getCompetitionId() != null) {
            Competition competition = competitionService.getById(request.getCompetitionId());
            if (competition != null) {
                if (competition.getMaxTeamsPerUser() != null && competition.getMaxTeamsPerUser() > 0) {
                    Long cnt = teamMemberMapper.countUserMembershipInCompetition(request.getLeaderId(), request.getCompetitionId());
                    if (cnt != null && cnt >= competition.getMaxTeamsPerUser()) {
                        throw new RuntimeException("该比赛每人最多可参加 " + competition.getMaxTeamsPerUser() + " 支队伍");
                    }
                }
                if (Boolean.TRUE.equals(competition.getEligibilityEnabled())) {
                    UserProfile profile = profileService.getProfileByUserId(request.getLeaderId());
                    if (!checkCompetitionAudienceEligibility(competition, profile)) {
                        throw new RuntimeException("当前比赛限定报名对象，不符合条件的同学无法发起队伍");
                    }
                }
            }
        }

        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setProjectId(request.getProjectId());
        team.setLeaderId(request.getLeaderId());
        team.setTeamNature("COMPETITION".equals(request.getType()) ? "LONG_TERM" : "TEMPORARY");
        team.setCompetitionId(request.getCompetitionId());
        team.setMaxMembers(request.getMaxMembers());
        team.setStatus("ACTIVE");
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());

        save(team);

        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(request.getLeaderId());
        member.setRole("LEADER");
        member.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);

        return team;
    }

    @SuppressWarnings("unchecked")
    private boolean checkCompetitionAudienceEligibility(Competition competition, UserProfile profile) {
        if (competition == null) return true;
        String audienceJson = competition.getAudience();
        if (audienceJson == null || audienceJson.isBlank()) return true;
        if (profile == null) return false;
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
            return true;
        }
    }

    @Override
    public List<Team> getUserTeams(Long userId) {
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getUserId, userId);
        List<TeamMember> members = teamMemberMapper.selectList(queryWrapper);

        List<Long> teamIds = members.stream().map(TeamMember::getTeamId).collect(Collectors.toList());

        LambdaQueryWrapper<Team> mentorQueryWrapper = new LambdaQueryWrapper<>();
        mentorQueryWrapper.eq(Team::getMentorId, userId);
        List<Team> mentorTeams = list(mentorQueryWrapper);

        Set<Long> allTeamIds = new HashSet<>(teamIds);
        mentorTeams.forEach(team -> allTeamIds.add(team.getId()));

        if (allTeamIds.isEmpty()) {
            return List.of();
        }

        return listByIds(allTeamIds);
    }

    @Override
    public Team getTeamById(Long teamId) {
        return getById(teamId);
    }

    @Override
    public void addMember(Long teamId, Long userId) {
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getTeamId, teamId).eq(TeamMember::getUserId, userId);
        if (teamMemberMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("User is already a member of this team");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(Long teamId, Long inviterId, Long inviteeId, String message) {
        // 验证团队存在
        Team team = getById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        
        // 验证邀请人是团队成员
        LambdaQueryWrapper<TeamMember> inviterQuery = new LambdaQueryWrapper<>();
        inviterQuery.eq(TeamMember::getTeamId, teamId)
                    .eq(TeamMember::getUserId, inviterId);
        if (teamMemberMapper.selectCount(inviterQuery) == 0) {
            throw new RuntimeException("只有团队成员才能邀请新成员");
        }
        
        // 检查被邀请人是否已经是团队成员
        LambdaQueryWrapper<TeamMember> inviteeQuery = new LambdaQueryWrapper<>();
        inviteeQuery.eq(TeamMember::getTeamId, teamId)
                    .eq(TeamMember::getUserId, inviteeId);
        if (teamMemberMapper.selectCount(inviteeQuery) > 0) {
            throw new RuntimeException("该用户已经是团队成员");
        }
        
        // 检查是否已有待处理的邀请
        LambdaQueryWrapper<TeamInvitation> invitationQuery = new LambdaQueryWrapper<>();
        invitationQuery.eq(TeamInvitation::getTeamId, teamId)
                       .eq(TeamInvitation::getInviteeId, inviteeId)
                       .eq(TeamInvitation::getStatus, "PENDING");
        if (teamInvitationMapper.selectCount(invitationQuery) > 0) {
            log.warn("该用户已有待处理的邀请: teamId={}, inviteeId={}", teamId, inviteeId);
            // 不抛出异常，但补发通知（避免历史邀请存在导致“永远收不到通知”）
            createTeamInvitationNotificationSafely(team, inviterId, inviteeId, message);
            return;
        }
        
        // 创建邀请记录
        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeamId(teamId);
        invitation.setInviterId(inviterId);
        invitation.setInviteeId(inviteeId);
        invitation.setMessage(message);
        invitation.setStatus("PENDING");
        invitation.setInvitedAt(LocalDateTime.now());
        // 设置7天后过期
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        
        teamInvitationMapper.insert(invitation);
        
        log.info("团队邀请已创建: teamId={}, inviterId={}, inviteeId={}", teamId, inviterId, inviteeId);

        // 创建通知（通知中心 + 小铃铛未读数）
        createTeamInvitationNotificationSafely(team, inviterId, inviteeId, message);
    }

    private void createTeamInvitationNotificationSafely(Team team, Long inviterId, Long inviteeId, String message) {
        try {
            User inviter = userService.getUserById(inviterId);
            String inviterName = inviter != null && inviter.getUsername() != null ? inviter.getUsername() : "用户";
            Long teamId = team.getId();
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("团队#" + teamId);
            String content = String.format("你收到来自 %s 的团队邀请：%s%s",
                    inviterName,
                    teamName,
                    (message != null && !message.isBlank()) ? ("\n留言：" + message) : "");
            
            log.info("准备创建团队邀请通知: inviteeId={}, inviterId={}, teamId={}, teamName={}", 
                    inviteeId, inviterId, teamId, teamName);
            
            notificationService.createNotification(
                    inviteeId,
                    "TEAM_INVITATION",
                    "团队邀请",
                    content,
                    "TEAM",
                    teamId
            );
            
            log.info("团队邀请通知创建成功: inviteeId={}, teamId={}", inviteeId, teamId);
        } catch (Exception e) {
            // 通知失败不影响邀请主流程，但记录详细错误信息
            log.error("创建团队邀请通知失败: teamId={}, inviterId={}, inviteeId={}, error={}", 
                    team != null ? team.getId() : null, inviterId, inviteeId, e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getInvitationDetail(Long invitationId, Long userId) {
        // 查询邀请记录
        TeamInvitation invitation = teamInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new RuntimeException("邀请不存在");
        }

        // 验证权限：只有邀请人或被邀请人可以查看
        if (!invitation.getInviterId().equals(userId) && !invitation.getInviteeId().equals(userId)) {
            throw new RuntimeException("无权限查看此邀请");
        }

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("id", invitation.getId());
        result.put("teamId", invitation.getTeamId());
        result.put("inviterId", invitation.getInviterId());
        result.put("inviteeId", invitation.getInviteeId());
        result.put("status", invitation.getStatus());
        result.put("message", invitation.getMessage());
        result.put("invitedAt", invitation.getInvitedAt());
        result.put("respondedAt", invitation.getRespondedAt());
        result.put("expiresAt", invitation.getExpiresAt());

        // 获取团队信息
        Team team = getById(invitation.getTeamId());
        if (team != null) {
            result.put("teamName", team.getTeamName());
            result.put("teamAvatar", team.getAvatar());
            result.put("teamDescription", team.getDescription());
        }

        // 获取邀请人信息
        try {
            User inviter = userService.getUserById(invitation.getInviterId());
            if (inviter != null) {
                result.put("inviterName", inviter.getUsername());
                UserProfile inviterProfile = profileService.getProfileByUserId(invitation.getInviterId());
                if (inviterProfile != null) {
                    result.put("inviterAvatar", inviterProfile.getAvatarUrl());
                }
            }
        } catch (Exception e) {
            log.warn("获取邀请人信息失败: inviterId={}", invitation.getInviterId());
        }

        return result;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvitation(Long invitationId, Long userId) {
        // 查询邀请记录
        TeamInvitation invitation = teamInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new RuntimeException("邀请不存在");
        }

        // 验证是否是被邀请人
        if (!invitation.getInviteeId().equals(userId)) {
            throw new RuntimeException("无权限接受此邀请");
        }

        // 验证邀请状态
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("邀请已处理或已过期");
        }

        // 检查邀请是否过期
        if (invitation.getExpiresAt() != null && LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            invitation.setStatus("EXPIRED");
            invitation.setUpdatedAt(LocalDateTime.now());
            teamInvitationMapper.updateById(invitation);
            throw new RuntimeException("邀请已过期");
        }

        // 验证团队存在
        Team team = getById(invitation.getTeamId());
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }

        // 检查是否已经是团队成员
        LambdaQueryWrapper<TeamMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(TeamMember::getTeamId, invitation.getTeamId())
                   .eq(TeamMember::getUserId, userId);
        if (teamMemberMapper.selectCount(memberQuery) > 0) {
            throw new RuntimeException("您已经是团队成员");
        }

        // 检查团队人数是否已满
        LambdaQueryWrapper<TeamMember> countQuery = new LambdaQueryWrapper<>();
        countQuery.eq(TeamMember::getTeamId, invitation.getTeamId());
        long currentMemberCount = teamMemberMapper.selectCount(countQuery);
        if (team.getMaxMembers() != null && currentMemberCount >= team.getMaxMembers()) {
            throw new RuntimeException("团队人数已满");
        }

        // 添加为团队成员
        TeamMember member = new TeamMember();
        member.setTeamId(invitation.getTeamId());
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);

        // 更新邀请状态
        invitation.setStatus("ACCEPTED");
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setUpdatedAt(LocalDateTime.now());
        teamInvitationMapper.updateById(invitation);

        // 发送通知给邀请人
        try {
            notificationService.createNotification(
                invitation.getInviterId(),
                "TEAM_INVITATION_ACCEPTED",
                "团队邀请已接受",
                String.format("用户已接受您的团队邀请，加入了团队：%s", team.getTeamName() != null ? team.getTeamName() : "队伍#" + team.getId()),
                "TEAM",
                invitation.getTeamId()
            );
        } catch (Exception e) {
            log.warn("发送邀请接受通知失败: invitationId={}, error={}", invitationId, e.getMessage());
        }

        log.info("用户接受团队邀请: invitationId={}, userId={}, teamId={}", invitationId, userId, invitation.getTeamId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectInvitation(Long invitationId, Long userId) {
        // 查询邀请记录
        TeamInvitation invitation = teamInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new RuntimeException("邀请不存在");
        }

        // 验证是否是被邀请人
        if (!invitation.getInviteeId().equals(userId)) {
            throw new RuntimeException("无权限拒绝此邀请");
        }

        // 验证邀请状态
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("邀请已处理或已过期");
        }

        // 更新邀请状态
        invitation.setStatus("REJECTED");
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setUpdatedAt(LocalDateTime.now());
        teamInvitationMapper.updateById(invitation);

        // 发送通知给邀请人
        try {
            Team team = getById(invitation.getTeamId());
            notificationService.createNotification(
                invitation.getInviterId(),
                "TEAM_INVITATION_REJECTED",
                "团队邀请已拒绝",
                String.format("用户拒绝了您的团队邀请：%s", team != null && team.getTeamName() != null ? team.getTeamName() : "队伍#" + invitation.getTeamId()),
                "TEAM",
                invitation.getTeamId()
            );
        } catch (Exception e) {
            log.warn("发送邀请拒绝通知失败: invitationId={}, error={}", invitationId, e.getMessage());
        }

        log.info("用户拒绝团队邀请: invitationId={}, userId={}, teamId={}", invitationId, userId, invitation.getTeamId());
    }

    @Override
    public List<Map<String, Object>> getSentInvitations(Long userId) {
        // 查询用户发出的邀请
        LambdaQueryWrapper<TeamInvitation> query = new LambdaQueryWrapper<>();
        query.eq(TeamInvitation::getInviterId, userId)
             .orderByDesc(TeamInvitation::getInvitedAt);
        List<TeamInvitation> invitations = teamInvitationMapper.selectList(query);

        // 构建返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeamInvitation invitation : invitations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", invitation.getId());
            item.put("teamId", invitation.getTeamId());
            item.put("inviteeId", invitation.getInviteeId());
            item.put("status", invitation.getStatus());
            item.put("message", invitation.getMessage());
            item.put("invitedAt", invitation.getInvitedAt());
            item.put("respondedAt", invitation.getRespondedAt());
            item.put("expiresAt", invitation.getExpiresAt());

            // 获取团队信息
            Team team = getById(invitation.getTeamId());
            if (team != null) {
                item.put("teamName", team.getTeamName());
                item.put("teamAvatar", team.getAvatar());
            }

            // 获取被邀请人信息
            try {
                User invitee = userService.getUserById(invitation.getInviteeId());
                if (invitee != null) {
                    item.put("inviteeName", invitee.getUsername());
                    UserProfile inviteeProfile = profileService.getProfileByUserId(invitation.getInviteeId());
                    if (inviteeProfile != null) {
                        item.put("inviteeAvatar", inviteeProfile.getAvatarUrl());
                    }
                }
            } catch (Exception e) {
                log.warn("获取被邀请人信息失败: inviteeId={}", invitation.getInviteeId());
            }

            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getReceivedInvitations(Long userId) {
        // 查询用户收到的邀请
        LambdaQueryWrapper<TeamInvitation> query = new LambdaQueryWrapper<>();
        query.eq(TeamInvitation::getInviteeId, userId)
             .orderByDesc(TeamInvitation::getInvitedAt);
        List<TeamInvitation> invitations = teamInvitationMapper.selectList(query);

        // 构建返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeamInvitation invitation : invitations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", invitation.getId());
            item.put("teamId", invitation.getTeamId());
            item.put("inviterId", invitation.getInviterId());
            item.put("status", invitation.getStatus());
            item.put("message", invitation.getMessage());
            item.put("invitedAt", invitation.getInvitedAt());
            item.put("respondedAt", invitation.getRespondedAt());
            item.put("expiresAt", invitation.getExpiresAt());

            // 获取团队信息
            Team team = getById(invitation.getTeamId());
            if (team != null) {
                item.put("teamName", team.getTeamName());
                item.put("teamAvatar", team.getAvatar());
                item.put("teamDescription", team.getDescription());
            }

            // 获取邀请人信息
            try {
                User inviter = userService.getUserById(invitation.getInviterId());
                if (inviter != null) {
                    item.put("inviterName", inviter.getUsername());
                    UserProfile inviterProfile = profileService.getProfileByUserId(invitation.getInviterId());
                    if (inviterProfile != null) {
                        item.put("inviterAvatar", inviterProfile.getAvatarUrl());
                    }
                }
            } catch (Exception e) {
                log.warn("获取邀请人信息失败: inviterId={}", invitation.getInviterId());
            }

            result.add(item);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInvitation(Long invitationId, Long userId) {
        // 查询邀请记录
        TeamInvitation invitation = teamInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new RuntimeException("邀请不存在");
        }

        // 验证是否是邀请人
        if (!invitation.getInviterId().equals(userId)) {
            throw new RuntimeException("只有邀请人可以撤回邀请");
        }

        // 验证邀请状态
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("只能撤回待处理的邀请");
        }

        // 更新邀请状态为CANCELLED
        invitation.setStatus("CANCELLED");
        invitation.setUpdatedAt(LocalDateTime.now());
        teamInvitationMapper.updateById(invitation);

        // 发送通知给被邀请人
        try {
            Team team = getById(invitation.getTeamId());
            notificationService.createNotification(
                invitation.getInviteeId(),
                "TEAM_INVITATION_CANCELLED",
                "团队邀请已撤回",
                String.format("团队邀请已被撤回：%s", team != null && team.getTeamName() != null ? team.getTeamName() : "队伍#" + invitation.getTeamId()),
                "TEAM",
                invitation.getTeamId()
            );
        } catch (Exception e) {
            log.warn("发送邀请撤回通知失败: invitationId={}, error={}", invitationId, e.getMessage());
        }

        log.info("邀请已撤回: invitationId={}, userId={}", invitationId, userId);
    }



    @Override
    public void removeMember(Long teamId, Long userId) {
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getTeamId, teamId).eq(TeamMember::getUserId, userId);
        teamMemberMapper.delete(queryWrapper);
    }

    @Override
    public List<TeamMemberVO> getTeamMembers(Long teamId) {
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getTeamId, teamId);
        List<TeamMember> members = teamMemberMapper.selectList(queryWrapper);

        List<TeamMemberVO> voList = new ArrayList<>();
        for (TeamMember member : members) {
            TeamMemberVO vo = new TeamMemberVO();
            BeanUtils.copyProperties(member, vo);
            try {
                User user = userService.getUserById(member.getUserId());
                if (user != null) {
                    vo.setUsername(user.getUsername());
                }
                UserProfile profile = profileService.getProfileByUserId(member.getUserId());
                if (profile != null) {
                    vo.setNickname(profile.getRealName());
                    vo.setAvatar(profile.getAvatarUrl());
                }
            } catch (Exception ignored) {
            }
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveTeam(Long teamId, Long userId) {
        Team team = getById(teamId);
        if (team == null) throw new RuntimeException("团队不存在");
        if (team.getLeaderId().equals(userId)) throw new RuntimeException("团队领导者不能直接退出，请先转让领导权或解散团队");

        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getTeamId, teamId).eq(TeamMember::getUserId, userId);
        if (teamMemberMapper.selectCount(queryWrapper) == 0) throw new RuntimeException("您不是该团队成员");

        teamMemberMapper.delete(queryWrapper);

        try {
            List<com.teamup.server.modules.team.entity.TeamProject> teamProjects = teamProjectService.getTeamProjects(teamId);
            boolean hasActiveProject = teamProjects.stream().anyMatch(tp -> "IN_PROGRESS".equals(tp.getStatus()) || "RECRUITING".equals(tp.getStatus()));
            if (hasActiveProject) {
                creditService.addCreditRecord(userId, -15, "PROJECT_QUIT", null, "中途退出团队: " + (team.getTeamName() != null ? team.getTeamName() : "队伍#" + teamId));
                log.info("用户{}中途退出团队{}，扣除15分信誉分", userId, teamId);
            }
        } catch (Exception e) {
            log.error("扣除退出团队信誉分失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeam(Long teamId, Long userId) {
        Team team = getById(teamId);
        if (team == null) throw new RuntimeException("团队不存在");
        if (!team.getLeaderId().equals(userId)) throw new RuntimeException("只有团队领导者可以删除团队");

        LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamMember::getTeamId, teamId);
        teamMemberMapper.delete(memberWrapper);
        removeById(teamId);
    }

    @Override
    public void updateTeamAvatar(Long teamId, String avatarUrl) {
        Team team = getById(teamId);
        if (team == null) throw new RuntimeException("团队不存在");
        team.setAvatar(avatarUrl);
        updateById(team);
    }

    @Override
    public Team updateTeam(Long teamId, Map<String, Object> updates) {
        Team team = getById(teamId);
        if (team == null) throw new RuntimeException("团队不存在");
        if (updates.containsKey("name")) team.setTeamName((String) updates.get("name"));
        if (updates.containsKey("teamName")) team.setTeamName((String) updates.get("teamName"));
        if (updates.containsKey("description")) team.setDescription((String) updates.get("description"));
        if (updates.containsKey("avatar")) team.setAvatar((String) updates.get("avatar"));
        updateById(team);
        return team;
    }

    @Override
    public Page<AdminTeamListVO> getAdminTeamList(Integer page, Integer size, String type, Boolean isActive, String keyword) {
        Page<Team> teamPage = new Page<>(page, size);
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            if ("COMPETITION".equals(type)) wrapper.eq(Team::getTeamNature, "LONG_TERM");
            else if ("PROJECT".equals(type)) wrapper.eq(Team::getTeamNature, "TEMPORARY");
        }
        if (isActive != null) {
            if (isActive) wrapper.eq(Team::getStatus, "ACTIVE");
            else wrapper.ne(Team::getStatus, "ACTIVE");
        }
        if (keyword != null && !keyword.isEmpty()) wrapper.like(Team::getTeamName, keyword);
        wrapper.orderByDesc(Team::getCreatedAt);
        Page<Team> result = page(teamPage, wrapper);

        Page<AdminTeamListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<AdminTeamListVO> voList = new ArrayList<>();
        for (Team team : result.getRecords()) {
            AdminTeamListVO vo = new AdminTeamListVO();
            vo.setId(team.getId());
            vo.setName(team.getTeamName());
            vo.setType("LONG_TERM".equals(team.getTeamNature()) ? "COMPETITION" : "PROJECT");
            vo.setLeaderId(team.getLeaderId());
            vo.setIsActive("ACTIVE".equals(team.getStatus()));
            vo.setCreatedAt(team.getCreatedAt());
            vo.setUpdatedAt(team.getUpdatedAt());
            try {
                User leader = userService.getUserById(team.getLeaderId());
                if (leader != null) vo.setLeaderName(leader.getUsername());
            } catch (Exception ignored) {
            }
            LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(TeamMember::getTeamId, team.getId());
            vo.setMemberCount(Math.toIntExact(teamMemberMapper.selectCount(memberWrapper)));
            vo.setProjectCount(0);
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public AdminTeamDetailVO getAdminTeamDetail(Long teamId) {
        Team team = getById(teamId);
        if (team == null) return null;

        AdminTeamDetailVO vo = new AdminTeamDetailVO();
        vo.setId(team.getId());
        vo.setName(team.getTeamName());
        vo.setDescription(team.getDescription());
        vo.setType("LONG_TERM".equals(team.getTeamNature()) ? "COMPETITION" : "PROJECT");
        vo.setSpecialization(null);
        vo.setIsActive("ACTIVE".equals(team.getStatus()));
        vo.setCreatedAt(team.getCreatedAt());
        vo.setUpdatedAt(team.getUpdatedAt());

        try {
            User leader = userService.getUserById(team.getLeaderId());
            if (leader != null) {
                AdminTeamDetailVO.LeaderInfo leaderInfo = new AdminTeamDetailVO.LeaderInfo();
                leaderInfo.setId(leader.getId());
                leaderInfo.setName(leader.getUsername());
                leaderInfo.setEmail(leader.getEmail());
                UserProfile profile = profileService.getProfileByUserId(leader.getId());
                if (profile != null) {
                    leaderInfo.setDepartment(profile.getDepartment());
                    leaderInfo.setMajor(profile.getMajor());
                }
                vo.setLeader(leaderInfo);
            }
        } catch (Exception ignored) {
        }

        List<AdminTeamDetailVO.MemberInfo> members = new ArrayList<>();
        LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamMember::getTeamId, teamId);
        List<TeamMember> teamMembers = teamMemberMapper.selectList(memberWrapper);

        for (TeamMember member : teamMembers) {
            AdminTeamDetailVO.MemberInfo memberInfo = new AdminTeamDetailVO.MemberInfo();
            memberInfo.setId(member.getId());
            memberInfo.setUserId(member.getUserId());
            memberInfo.setRole(member.getRole());
            memberInfo.setJoinedAt(member.getJoinedAt());
            memberInfo.setLeftAt(null);
            try {
                User u = userService.getUserById(member.getUserId());
                if (u != null) memberInfo.setUserName(u.getUsername());
            } catch (Exception ignored) {
            }
            members.add(memberInfo);
        }
        vo.setMembers(members);
        vo.setProjects(new ArrayList<>());
        return vo;
    }

    @Override
    public List<TeamMember> getTeamMembersByTeamId(Long teamId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId);
        return teamMemberMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getAdminTeamStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long totalTeams = count();
        stats.put("totalTeams", totalTeams);
        stats.put("activeTeams", totalTeams);
        long totalMembers = teamMemberMapper.selectCount(null);
        stats.put("averageMemberCount", totalTeams > 0 ? (double) totalMembers / totalTeams : 0.0);
        stats.put("totalProjects", 0L);
        return stats;
    }

    @Override
    public List<Map<String, Object>> getTeamCompetitions(Long teamId) {
        List<Long> competitionIds = baseMapper.selectTeamCompetitionIds(teamId);
        if (competitionIds == null || competitionIds.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> competitions = new ArrayList<>();
        for (Long competitionId : competitionIds) {
            Competition competition = competitionService.getById(competitionId);
            if (competition != null) {
                Map<String, Object> compMap = new HashMap<>();
                compMap.put("id", competition.getId());
                compMap.put("name", competition.getName());
                compMap.put("status", competition.getStatus());
                compMap.put("startDate", competition.getStartAt());
                compMap.put("endDate", competition.getEndAt());
                compMap.put("description", competition.getDescription());
                competitions.add(compMap);
            }
        }
        return competitions;
    }

    /**
     * 成员找团队：为当前用户匹配长期团队
     * @deprecated 此功能已废弃，直接返回空列表
     */
    @Deprecated
    @Override
    public List<Map<String, Object>> matchTeamsForUser(Long userId, int page, int size) {
        // 功能已废弃，返回空列表
        log.warn("matchTeamsForUser 方法已废弃，userId: {}", userId);
        return new ArrayList<>();
    }

    /**
     * 成员找团队：为当前用户匹配长期团队（原实现，已废弃）
     * 保留代码仅供参考，未来版本将移除
     */
    @Deprecated
    @SuppressWarnings("unused")
    private List<Map<String, Object>> matchTeamsForUserDeprecated(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 50));

        User user = userService.getUserById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        List<Long> joinedTeamIds = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId)
        ).stream().map(TeamMember::getTeamId).collect(Collectors.toList());

        List<Team> candidates = list(new LambdaQueryWrapper<Team>()
                .eq(Team::getStatus, "ACTIVE")
                .eq(Team::getTeamNature, "LONG_TERM")
                .orderByDesc(Team::getCreatedAt));

        List<Team> filtered = candidates.stream()
                .filter(team -> team.getLeaderId() == null || !team.getLeaderId().equals(userId))
                .filter(team -> !joinedTeamIds.contains(team.getId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> userPayload = buildTeamMatchUserPayload(userId, user);
        List<Map<String, Object>> teamPayloads = filtered.stream().map(this::buildTeamMatchTeamPayload).collect(Collectors.toList());

        UserTeamMatchRequest req = new UserTeamMatchRequest();
        req.setUserId(userId);
        req.setUser(userPayload);
        req.setTeams(teamPayloads);

        List<UserTeamMatchResult> results;
        try {
            results = matchingFeignClient.matchUserToTeams(req);
        } catch (Exception e) {
            log.error("调用匹配服务 user-to-teams 失败", e);
            return new ArrayList<>();
        }

        Map<Long, Team> teamMap = filtered.stream().collect(Collectors.toMap(Team::getId, t -> t));
        return results.stream()
                .filter(r -> r.getTeamId() != null)
                .map(r -> {
                    Team team = teamMap.get(r.getTeamId());
                    if (team == null) return null;
                    int memberCount = Math.toIntExact(teamMemberMapper.selectCount(
                            new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, team.getId())
                    ));
                    int maxMembers = team.getMaxMembers() != null && team.getMaxMembers() > 0 ? team.getMaxMembers() : 6;

                    Map<String, Object> item = new HashMap<>();
                    item.put("team", team);
                    item.put("teamId", team.getId());
                    item.put("matchScore", r.getMatchScore() != null ? r.getMatchScore() : 0D);
                    item.put("breakdown", r.getBreakdown() != null ? r.getBreakdown() : new HashMap<>());
                    item.put("matchReason", r.getMatchReason() != null ? r.getMatchReason() : "综合评估推荐");
                    item.put("memberCount", memberCount);
                    item.put("maxMembers", maxMembers);
                    return item;
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> Double.compare(
                        ((Number) b.get("matchScore")).doubleValue(),
                        ((Number) a.get("matchScore")).doubleValue()))
                .skip((long) (safePage - 1) * safeSize)
                .limit(safeSize)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildTeamMatchUserPayload(Long userId, User user) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());

        UserProfile profile = profileService.getProfileByUserId(userId);
        if (profile != null) {
            userInfo.put("bio", profile.getBio());
            userInfo.put("project_experience", profile.getProjectExperience());
            userInfo.put("department", profile.getDepartment());
            userInfo.put("major", profile.getMajor());
            userInfo.put("grade", profile.getGrade());
        }

        List<String> interests = userInterestMapper.selectList(
                new LambdaQueryWrapper<UserInterest>().eq(UserInterest::getUserId, userId)
        ).stream().map(UserInterest::getInterestName).collect(Collectors.toList());
        userInfo.put("interests", interests);

        payload.put("user", userInfo);

        List<Map<String, Object>> skills = new ArrayList<>();
        List<UserTag> userTags = userTagMapper.selectList(new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId));
        if (!userTags.isEmpty()) {
            Set<Long> tagIds = userTags.stream().map(UserTag::getTagId).collect(Collectors.toSet());
            Map<Long, Tag> tags = tagMapper.selectBatchIds(tagIds).stream().collect(Collectors.toMap(Tag::getId, t -> t));
            for (UserTag ut : userTags) {
                Tag t = tags.get(ut.getTagId());
                if (t == null || !"SKILL".equals(t.getCategory())) continue;
                Map<String, Object> s = new HashMap<>();
                s.put("skill_name", t.getName());
                s.put("proficiency_level", ut.getProficiencyLevel());
                s.put("certification_type", Boolean.TRUE.equals(ut.getIsVerified()) ? "OFFICIAL" : "SELF_CLAIM");
                skills.add(s);
            }
        }
        payload.put("skills", skills);

        List<Map<String, Object>> availability = userAvailabilityMapper.selectList(
                new LambdaQueryWrapper<UserAvailability>().eq(UserAvailability::getUserId, userId)
        ).stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("day_of_week", a.getDayOfWeek());
            m.put("start_time", a.getStartTime().toString());
            m.put("end_time", a.getEndTime().toString());
            return m;
        }).collect(Collectors.toList());
        payload.put("availability", availability);

        payload.put("credit", new HashMap<>());
        payload.put("collaboration_history", new ArrayList<>());

        return payload;
    }

    private Map<String, Object> buildTeamMatchTeamPayload(Team team) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", team.getId());
        m.put("title", team.getTeamName());
        m.put("description", team.getDescription());
        m.put("project_type", "LONG_TERM_TEAM");
        m.put("weekly_hours", 10);
        m.put("creator_id", team.getLeaderId());
        m.put("skill_requirements", new ArrayList<>());
        m.put("time_slots", new ArrayList<>());
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTeamCompetition(Long teamId, Long competitionId) {
        Team team = getById(teamId);
        if (team == null) throw new RuntimeException("团队不存在");

        Competition competition = competitionService.getById(competitionId);
        if (competition == null) throw new RuntimeException("比赛不存在");

        if (baseMapper.isTeamCompetitionExists(teamId, competitionId)) {
            throw new RuntimeException("该比赛已经关联过了");
        }

        baseMapper.insertTeamCompetition(teamId, competitionId);
        log.info("团队 {} 关联比赛 {} 成功", teamId, competitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTeamCompetition(Long teamId, Long competitionId) {
        Team team = getById(teamId);
        if (team == null) throw new RuntimeException("团队不存在");

        if (!baseMapper.isTeamCompetitionExists(teamId, competitionId)) {
            throw new RuntimeException("该比赛未关联");
        }

        baseMapper.deleteTeamCompetition(teamId, competitionId);
        log.info("团队 {} 移除比赛 {} 关联成功", teamId, competitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveTeam(Long teamId, Long userId) {
        Team team = getById(teamId);
        if (team == null) throw new RuntimeException("团队不存在");
        if (!team.getLeaderId().equals(userId)) throw new RuntimeException("只有队长可以解散团队");

        team.setStatus("DISSOLVED");
        team.setUpdatedAt(LocalDateTime.now());
        updateById(team);

        try {
            List<TeamMemberVO> members = getTeamMembers(teamId);
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("团队#" + teamId);
            for (TeamMemberVO member : members) {
                if (!member.getUserId().equals(userId)) {
                    notificationService.createNotification(
                            member.getUserId(),
                            "TEAM_DISSOLVED",
                            "团队已解散",
                            "你所在的团队「" + teamName + "」已被队长解散",
                            "TEAM",
                            teamId
                    );
                }
            }
        } catch (Exception e) {
            log.error("发送团队解散通知失败", e);
        }

        log.info("团队 {} 已解散", teamId);
    }
}
