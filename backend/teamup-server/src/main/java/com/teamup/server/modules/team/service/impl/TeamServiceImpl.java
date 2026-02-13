package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.team.dto.TeamCreateRequest;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.team.vo.AdminTeamDetailVO;
import com.teamup.server.modules.team.vo.AdminTeamListVO;
import com.teamup.server.modules.team.vo.TeamMemberVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.service.CompetitionService;
import com.teamup.server.modules.user.service.UserService;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.service.ProfileService;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    private final TeamMemberMapper teamMemberMapper;
    private final UserService userService;
    private final ProfileService profileService;
    private final CompetitionService competitionService;
    private final ProjectService projectService;
    private final com.teamup.server.modules.notification.service.NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Team createTeam(TeamCreateRequest request) {
        // 比赛约束：人数上限 & 报名资格
        if ("COMPETITION".equals(request.getType()) && request.getCompetitionId() != null) {
            Competition competition = competitionService.getById(request.getCompetitionId());
            if (competition != null) {
                // 同一比赛每个用户可参加的队伍上限
                if (competition.getMaxTeamsPerUser() != null && competition.getMaxTeamsPerUser() > 0) {
                Long cnt = teamMemberMapper.countUserMembershipInCompetition(request.getLeaderId(), request.getCompetitionId());
                if (cnt != null && cnt >= competition.getMaxTeamsPerUser()) {
                    throw new RuntimeException("该比赛每人最多可参加 " + competition.getMaxTeamsPerUser() + " 支队伍");
                    }
                }
                // 报名资格校验（按 audience + user profile）
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
        // 根据type设置team_nature：PROJECT->TEMPORARY, COMPETITION->LONG_TERM
        if ("COMPETITION".equals(request.getType())) {
            team.setTeamNature("LONG_TERM");
        } else {
            team.setTeamNature("TEMPORARY");
        }
        team.setCompetitionId(request.getCompetitionId());
        team.setMaxMembers(request.getMaxMembers());
        team.setStatus("ACTIVE"); // 设置默认状态
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());
        
        save(team);

        // Add leader as a member
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(request.getLeaderId());
        member.setRole("LEADER");
        member.setJoinedAt(LocalDateTime.now());
        
        teamMemberMapper.insert(member);
        
        return team;
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

            // 三个维度都满足（或该维度未配置）才视为通过
            return deptOk && majorOk && gradeOk;
        } catch (Exception e) {
            // 配置异常时不阻断报名，视为通过
            return true;
        }
    }

    @Override
    public List<Team> getUserTeams(Long userId) {
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getUserId, userId);
        List<TeamMember> members = teamMemberMapper.selectList(queryWrapper);
        
        if (members.isEmpty()) {
            return List.of();
        }
        
        List<Long> teamIds = members.stream()
                .map(TeamMember::getTeamId)
                .collect(Collectors.toList());
                
        return listByIds(teamIds);
    }

    @Override
    public Team getTeamById(Long teamId) {
        return getById(teamId);
    }

    @Override
    public void addMember(Long teamId, Long userId) {
        // 检查是否已存在
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId);
        if (teamMemberMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("User is already a member of this team");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);
        
        // 🔔 通知现有成员和新成员
        try {
            Team team = getById(teamId);
            User newMember = userService.getUserById(userId);
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("队伍#" + teamId);
            String newMemberName = newMember != null ? newMember.getUsername() : "新成员";
            
            // 通知所有现有成员（不包括新成员）
            List<TeamMemberVO> members = getTeamMembers(teamId);
            for (TeamMemberVO existingMember : members) {
                if (!existingMember.getUserId().equals(userId)) {
                    notificationService.createNotification(
                        existingMember.getUserId(),
                        "TEAM_MEMBER_JOINED",
                        newMemberName + " 加入了团队",
                        "团队：" + teamName,
                        "TEAM",
                        teamId
                    );
                }
            }
            
            // 欢迎新成员
            notificationService.createNotification(
                userId,
                "TEAM_WELCOME",
                "欢迎加入团队 " + teamName,
                "开始你的团队协作之旅吧！",
                "TEAM",
                teamId
            );
        } catch (Exception e) {
            log.error("发送成员加入通知失败", e);
        }
    }

    @Override
    public void removeMember(Long teamId, Long userId) {
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId);
        teamMemberMapper.delete(queryWrapper);
        
        // 🔔 通知被移除的成员和其他成员
        try {
            Team team = getById(teamId);
            User removedUser = userService.getUserById(userId);
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("队伍#" + teamId);
            String removedUserName = removedUser != null ? removedUser.getUsername() : "成员";
            
            // 通知被移除的成员
            notificationService.createNotification(
                userId,
                "TEAM_MEMBER_REMOVED",
                "你已被移出团队",
                "团队：" + teamName,
                "TEAM",
                teamId
            );
            
            // 通知其他成员
            List<TeamMemberVO> members = getTeamMembers(teamId);
            for (TeamMemberVO member : members) {
                notificationService.createNotification(
                    member.getUserId(),
                    "TEAM_MEMBER_LEFT",
                    removedUserName + " 离开了团队",
                    "团队：" + teamName,
                    "TEAM",
                    teamId
                );
            }
        } catch (Exception e) {
            log.error("发送成员移除通知失败", e);
        }
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
            
            // 调用 User Service 获取用户信息
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
            } catch (Exception e) {
                // 降级处理，或者记录日志
                System.err.println("Failed to get user info for user " + member.getUserId());
            }
            
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveTeam(Long teamId, Long userId) {
        // 检查团队是否存在
        Team team = getById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        
        // 检查是否为团队领导者
        if (team.getLeaderId().equals(userId)) {
            throw new RuntimeException("团队领导者不能直接退出，请先转让领导权或解散团队");
        }
        
        // 检查是否为团队成员
        LambdaQueryWrapper<TeamMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId);
        if (teamMemberMapper.selectCount(queryWrapper) == 0) {
            throw new RuntimeException("您不是该团队成员");
        }
        
        // 移除成员
        teamMemberMapper.delete(queryWrapper);
        
        // 🔔 通知队长和其他成员
        try {
            User leavingUser = userService.getUserById(userId);
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("队伍#" + teamId);
            String leavingUserName = leavingUser != null ? leavingUser.getUsername() : "成员";
            
            List<TeamMemberVO> members = getTeamMembers(teamId);
            for (TeamMemberVO member : members) {
                if (!member.getUserId().equals(userId)) {
                    notificationService.createNotification(
                        member.getUserId(),
                        "TEAM_MEMBER_LEFT",
                        leavingUserName + " 退出了团队",
                        "团队：" + teamName,
                        "TEAM",
                        teamId
                    );
                }
            }
        } catch (Exception e) {
            log.error("发送成员退出通知失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeam(Long teamId, Long userId) {
        // 检查团队是否存在
        Team team = getById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        
        // 检查是否为团队领导者
        if (!team.getLeaderId().equals(userId)) {
            throw new RuntimeException("只有团队领导者可以删除团队");
        }
        
        // 🔔 通知所有成员（在删除前）
        try {
            String teamName = team.getTeamName() != null ? team.getTeamName() : ("队伍#" + teamId);
            List<TeamMemberVO> members = getTeamMembers(teamId);
            for (TeamMemberVO member : members) {
                if (!member.getUserId().equals(userId)) {
                    notificationService.createNotification(
                        member.getUserId(),
                        "TEAM_DISBANDED",
                        "团队已解散",
                        "团队 " + teamName + " 已被队长解散",
                        "TEAM",
                        teamId
                    );
                }
            }
        } catch (Exception e) {
            log.error("发送团队解散通知失败", e);
        }
        
        // 删除所有团队成员
        LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamMember::getTeamId, teamId);
        teamMemberMapper.delete(memberWrapper);
        
        // 删除团队
        removeById(teamId);
    }
    
    @Override
    public void updateTeamAvatar(Long teamId, String avatarUrl) {
        Team team = getById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        team.setAvatar(avatarUrl);
        updateById(team);
    }
    
    @Override
    public Team updateTeam(Long teamId, java.util.Map<String, Object> updates) {
        Team team = getById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        
        if (updates.containsKey("name")) {
            team.setTeamName((String) updates.get("name"));
        }
        if (updates.containsKey("teamName")) {
            team.setTeamName((String) updates.get("teamName"));
        }
        if (updates.containsKey("description")) {
            team.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("avatar")) {
            team.setAvatar((String) updates.get("avatar"));
        }
        
        updateById(team);
        return team;
    }

    @Override
    public Page<AdminTeamListVO> getAdminTeamList(Integer page, Integer size, String type, Boolean isActive, String keyword) {
        Page<Team> teamPage = new Page<>(page, size);
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        
        // type参数映射：PROJECT->TEMPORARY, COMPETITION->LONG_TERM
        if (type != null && !type.isEmpty()) {
            if ("COMPETITION".equals(type)) {
                wrapper.eq(Team::getTeamNature, "LONG_TERM");
            } else if ("PROJECT".equals(type)) {
                wrapper.eq(Team::getTeamNature, "TEMPORARY");
            }
        }
        
        // 使用status字段过滤活跃状态
        if (isActive != null) {
            if (isActive) {
                wrapper.eq(Team::getStatus, "ACTIVE");
            } else {
                wrapper.ne(Team::getStatus, "ACTIVE");
            }
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Team::getTeamName, keyword);
        }
        
        wrapper.orderByDesc(Team::getCreatedAt);
        Page<Team> result = page(teamPage, wrapper);
        
        // 转换为VO
        Page<AdminTeamListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<AdminTeamListVO> voList = new ArrayList<>();
        
        for (Team team : result.getRecords()) {
            AdminTeamListVO vo = new AdminTeamListVO();
            vo.setId(team.getId());
            vo.setName(team.getTeamName());
            // 将team_nature转换回type：TEMPORARY->PROJECT, LONG_TERM->COMPETITION
            if ("LONG_TERM".equals(team.getTeamNature())) {
                vo.setType("COMPETITION");
            } else {
                vo.setType("PROJECT");
            }
            vo.setLeaderId(team.getLeaderId());
            vo.setIsActive("ACTIVE".equals(team.getStatus()));
            vo.setCreatedAt(team.getCreatedAt());
            vo.setUpdatedAt(team.getUpdatedAt());
            
            // 获取领导者姓名
            try {
                User leader = userService.getUserById(team.getLeaderId());
                if (leader != null) {
                    vo.setLeaderName(leader.getUsername());
                }
            } catch (Exception e) {
                log.error("获取团队领导者信息失败", e);
            }
            
            // 统计成员数量
            LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(TeamMember::getTeamId, team.getId());
            vo.setMemberCount(Math.toIntExact(teamMemberMapper.selectCount(memberWrapper)));
            
            // 统计项目数量 - Project doesn't have teamId field, skip this
            vo.setProjectCount(0);
            
            voList.add(vo);
        }
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    @Override
    public AdminTeamDetailVO getAdminTeamDetail(Long teamId) {
        Team team = getById(teamId);
        if (team == null) {
            return null;
        }
        
        AdminTeamDetailVO vo = new AdminTeamDetailVO();
        vo.setId(team.getId());
        vo.setName(team.getTeamName());
        vo.setDescription(team.getDescription());
        // 将team_nature转换回type：TEMPORARY->PROJECT, LONG_TERM->COMPETITION
        if ("LONG_TERM".equals(team.getTeamNature())) {
            vo.setType("COMPETITION");
        } else {
            vo.setType("PROJECT");
        }
        vo.setSpecialization(null); // Team doesn't have this field
        vo.setIsActive("ACTIVE".equals(team.getStatus()));
        vo.setCreatedAt(team.getCreatedAt());
        vo.setUpdatedAt(team.getUpdatedAt());
        
        // 获取领导者信息
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
        } catch (Exception e) {
            log.error("获取团队领导者信息失败", e);
        }
        
        // 获取成员列表
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
            memberInfo.setLeftAt(null); // TeamMember doesn't have this field
            
            try {
                User user = userService.getUserById(member.getUserId());
                if (user != null) {
                    memberInfo.setUserName(user.getUsername());
                }
            } catch (Exception e) {
                log.error("获取成员信息失败", e);
            }
            
            members.add(memberInfo);
        }
        vo.setMembers(members);
        
        // 获取项目列表 - Project doesn't have teamId field, return empty list
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
        
        // 总团队数
        long totalTeams = count();
        stats.put("totalTeams", totalTeams);
        
        // 活跃团队数 (all teams are considered active since isActive field doesn't exist)
        stats.put("activeTeams", totalTeams);
        
        // 平均成员数
        long totalMembers = teamMemberMapper.selectCount(null);
        stats.put("averageMemberCount", totalTeams > 0 ? (double) totalMembers / totalTeams : 0.0);
        
        // 总项目数（团队关联的项目）- Project doesn't have teamId field
        stats.put("totalProjects", 0L);
        
        return stats;
    }
}
