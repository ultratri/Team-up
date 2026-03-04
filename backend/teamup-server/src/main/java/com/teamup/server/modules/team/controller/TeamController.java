package com.teamup.server.modules.team.controller;

import com.teamup.server.modules.team.dto.TeamCreateRequest;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.project.dto.matching.MatchResult;
import com.teamup.server.modules.project.service.MatchingIntegrationService;
import com.teamup.server.modules.team.service.TeamStatisticsService;
import com.teamup.server.modules.team.vo.TeamStatisticsVO;
import com.teamup.server.modules.team.vo.TeamVO;
import com.teamup.server.modules.user.security.UserContext;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.service.UserService;
import com.teamup.server.modules.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.teamup.server.modules.team.vo.TeamMemberVO;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamStatisticsService statisticsService;
    private final TeamMemberMapper teamMemberMapper;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final ProfileService profileService;
    private final MatchingIntegrationService matchingIntegrationService;

    @PostMapping
    public Result<Team> createTeam(@RequestBody TeamCreateRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (hasRole(currentUserId, "MENTOR")) {
            return Result.error(403, "导师不能创建团队");
        }
        return Result.success(teamService.createTeam(request));
    }

    @GetMapping("/{id}")
    public Result<TeamVO> getTeam(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        if (team == null) {
            return Result.error(404, "团队不存在");
        }
        
        TeamVO vo = TeamVO.fromEntity(team);
        
        if (team.getMentorId() != null) {
            User mentor = userService.getUserById(team.getMentorId());
            if (mentor != null) {
                UserProfile profile = profileService.getProfileByUserId(team.getMentorId());
                TeamVO.MentorInfo mentorInfo = new TeamVO.MentorInfo();
                mentorInfo.setId(mentor.getId());
                mentorInfo.setName(mentor.getUsername());
                if (profile != null) {
                    mentorInfo.setAvatar(profile.getAvatarUrl());
                    mentorInfo.setDepartment(profile.getDepartment());
                    mentorInfo.setMajor(profile.getMajor());
                }
                vo.setMentor(mentorInfo);
            }
        }
        
        return Result.success(vo);
    }

    @GetMapping("/user/{userId}")
    public Result<List<TeamVO>> getUserTeams(@PathVariable Long userId) {
        List<Team> teams = teamService.getUserTeams(userId);
        
        List<TeamVO> teamVOs = teams.stream().map(team -> {
            TeamVO vo = TeamVO.fromEntity(team);
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.eq("team_id", team.getId());
            Long memberCount = teamMemberMapper.selectCount(wrapper);
            vo.setMemberCount(memberCount.intValue());
            return vo;
        }).collect(Collectors.toList());
        
        return Result.success(teamVOs);
    }

    /**
     * 成员找团队（长期团队推荐）
     * @deprecated 此功能已废弃，建议使用"成员找项目"功能
     * 保留此接口仅为向后兼容，未来版本将移除
     */
    @Deprecated
    @GetMapping("/match-for-me")
    public Result<List<Map<String, Object>>> matchTeamsForCurrentUser(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // 返回空列表，不再提供团队推荐功能
        return Result.success(new ArrayList<>());
    }

    /**
     * 团队找成员：为指定团队推荐候选人
     */
    @PostMapping("/{teamId}/match")
    public Result<List<MatchResult>> matchCandidatesForTeam(
            @PathVariable Long teamId,
            @RequestParam(required = false) String keyword
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        Team team = teamService.getTeamById(teamId);
        if (team == null) {
            return Result.error(404, "团队不存在");
        }
        if (!currentUserId.equals(team.getLeaderId())) {
            return Result.error(403, "只有团队领导者可以使用团队找成员功能");
        }

        List<MatchResult> results = matchingIntegrationService.matchTeamCandidates(teamId, keyword);
        return Result.success(results != null ? results : new ArrayList<>());
    }

    @PostMapping("/{teamId}/members")
    public Result<Void> addMember(@PathVariable Long teamId, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        teamService.addMember(teamId, userId);
        return Result.success();
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        teamService.removeMember(teamId, userId);
        return Result.success();
    }

    @GetMapping("/{teamId}/members")
    public Result<List<TeamMemberVO>> getTeamMembers(@PathVariable Long teamId) {
        return Result.success(teamService.getTeamMembers(teamId));
    }

    @GetMapping("/{teamId}/statistics")
    public Result<TeamStatisticsVO> getTeamStatistics(@PathVariable Long teamId) {
        try {
            Team team = teamService.getTeamById(teamId);
            if (team == null) {
                return Result.error(404, "团队不存在");
            }
            
            Long currentUserId;
            try {
                currentUserId = UserContext.getCurrentUserId();
            } catch (Exception e) {
                return Result.error(401, "请先登录");
            }
            
            // 修改权限逻辑：允许所有登录用户查看团队统计信息
            // 这有助于用户在申请加入前了解团队情况（团队推荐场景）
            // 统计数据不包含敏感信息，可以公开给所有登录用户
            
            TeamStatisticsVO statistics = statisticsService.calculateStatistics(teamId);
            return Result.success(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取团队统计失败: " + e.getMessage());
        }
    }
    
    private boolean isTeamMember(Long teamId, Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("user_id", userId);
        if (teamMemberMapper.selectCount(wrapper) > 0) {
            return true;
        }
        
        Team team = teamService.getTeamById(teamId);
        return team != null && userId.equals(team.getMentorId());
    }
    
    private boolean hasRole(Long userId, String roleName) {
        User user = userService.getUserById(userId);
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains(roleName);
    }

    @DeleteMapping("/{teamId}/leave")
    public Result<Void> leaveTeam(@PathVariable Long teamId) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            teamService.leaveTeam(teamId, currentUserId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{teamId}")
    public Result<Void> deleteTeam(@PathVariable Long teamId) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            teamService.deleteTeam(teamId, currentUserId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
    
    @PostMapping("/{teamId}/avatar")
    public Result<Map<String, String>> uploadTeamAvatar(
            @PathVariable Long teamId,
            @RequestParam("file") MultipartFile file) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        if (!isTeamMember(teamId, currentUserId)) {
            return Result.error(403, "无权限修改该团队");
        }
        
        try {
            String avatarUrl = fileStorageService.uploadFile(file, "team-avatar");
            teamService.updateTeamAvatar(teamId, avatarUrl);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", avatarUrl);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "头像上传失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{teamId}")
    public Result<Team> updateTeam(@PathVariable Long teamId, @RequestBody Map<String, Object> updates) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        if (!isTeamMember(teamId, currentUserId)) {
            return Result.error(403, "无权限修改该团队");
        }
        
        try {
            Team team = teamService.updateTeam(teamId, updates);
            return Result.success(team);
        } catch (Exception e) {
            return Result.error(500, "更新团队失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{teamId}/competitions")
    public Result<List<Map<String, Object>>> getTeamCompetitions(@PathVariable Long teamId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        if (!isTeamMember(teamId, currentUserId)) {
            return Result.error(403, "无权限访问该团队");
        }
        
        try {
            List<Map<String, Object>> competitions = teamService.getTeamCompetitions(teamId);
            return Result.success(competitions);
        } catch (Exception e) {
            return Result.error(500, "获取关联比赛失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{teamId}/competitions/{competitionId}")
    public Result<Void> addTeamCompetition(
            @PathVariable Long teamId, 
            @PathVariable Long competitionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        if (!isTeamLeader(teamId, currentUserId)) {
            return Result.error(403, "只有团队领导者可以关联比赛");
        }
        
        try {
            teamService.addTeamCompetition(teamId, competitionId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "关联比赛失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{teamId}/competitions/{competitionId}")
    public Result<Void> removeTeamCompetition(
            @PathVariable Long teamId, 
            @PathVariable Long competitionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        if (!isTeamLeader(teamId, currentUserId)) {
            return Result.error(403, "只有团队领导者可以移除比赛关联");
        }
        
        try {
            teamService.removeTeamCompetition(teamId, competitionId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "移除比赛关联失败: " + e.getMessage());
        }
    }
    
    private boolean isTeamLeader(Long teamId, Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("user_id", userId);
        wrapper.in("role", "LEADER", "OWNER", "ADMIN");
        return teamMemberMapper.selectCount(wrapper) > 0;
    }


    /**
     * 获取邀请详情
     */
    @GetMapping("/invitations/{invitationId}")
    public Result<Map<String, Object>> getInvitation(@PathVariable Long invitationId) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            Map<String, Object> invitation = teamService.getInvitationDetail(invitationId, currentUserId);
            return Result.success(invitation);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
    
    /**
     * 接受团队邀请
     */
    @PostMapping("/invitations/{invitationId}/accept")
    public Result<Void> acceptInvitation(@PathVariable Long invitationId) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            teamService.acceptInvitation(invitationId, currentUserId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 拒绝团队邀请
     */
    @PostMapping("/invitations/{invitationId}/reject")
    public Result<Void> rejectInvitation(@PathVariable Long invitationId) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            teamService.rejectInvitation(invitationId, currentUserId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取我发出的邀请列表
     */
    @GetMapping("/invitations/sent")
    public Result<List<Map<String, Object>>> getSentInvitations() {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            List<Map<String, Object>> invitations = teamService.getSentInvitations(currentUserId);
            return Result.success(invitations);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取我收到的邀请列表
     */
    @GetMapping("/invitations/received")
    public Result<List<Map<String, Object>>> getReceivedInvitations() {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            List<Map<String, Object>> invitations = teamService.getReceivedInvitations(currentUserId);
            return Result.success(invitations);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
    
    /**
     * 撤回邀请
     */
    @PostMapping("/invitations/{invitationId}/cancel")
    public Result<Void> cancelInvitation(@PathVariable Long invitationId) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            teamService.cancelInvitation(invitationId, currentUserId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
