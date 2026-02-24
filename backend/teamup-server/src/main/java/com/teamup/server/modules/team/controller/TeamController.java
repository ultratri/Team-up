package com.teamup.server.modules.team.controller;

import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.team.dto.TeamCreateRequest;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.common.utils.Result;
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

    @PostMapping
    public Result<Team> createTeam(@RequestBody TeamCreateRequest request) {
        // 检查当前用户角色，导师不能创建团队
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
        
        // 转换为 VO
        TeamVO vo = TeamVO.fromEntity(team);
        
        // 填充导师信息
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
        
        // 转换为 TeamVO 并添加成员数量
        List<TeamVO> teamVOs = teams.stream().map(team -> {
            TeamVO vo = TeamVO.fromEntity(team);
            
            // 查询成员数量
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.eq("team_id", team.getId());
            Long memberCount = teamMemberMapper.selectCount(wrapper);
            vo.setMemberCount(memberCount.intValue());
            
            return vo;
        }).collect(Collectors.toList());
        
        return Result.success(teamVOs);
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

    /**
     * 获取团队统计数据
     * @param teamId 团队ID
     * @return 团队统计数据
     */
    /**
     * 获取团队统计数据
     * @param teamId 团队ID
     * @return 团队统计数据
     */
    @GetMapping("/{teamId}/statistics")
    public Result<TeamStatisticsVO> getTeamStatistics(@PathVariable Long teamId) {
        try {
            // 验证团队是否存在
            Team team = teamService.getTeamById(teamId);
            if (team == null) {
                return Result.error(404, "团队不存在");
            }
            
            // 尝试获取当前用户ID进行权限验证
            Long currentUserId = null;
            try {
                currentUserId = UserContext.getCurrentUserId();
            } catch (Exception e) {
                System.err.println("获取用户ID失败: " + e.getMessage());
                System.err.println("SecurityContext: " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
                return Result.error(401, "请先登录");
            }
            
            if (!isTeamMember(teamId, currentUserId)) {
                return Result.error(403, "无权限访问该团队数据");
            }
            
            // 计算并返回统计数据
            TeamStatisticsVO statistics = statisticsService.calculateStatistics(teamId);
            return Result.success(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取团队统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查用户是否为团队成员或导师
     */
    private boolean isTeamMember(Long teamId, Long userId) {
        // 检查是否为团队成员
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("user_id", userId);
        if (teamMemberMapper.selectCount(wrapper) > 0) {
            return true;
        }
        
        // 检查是否为团队导师
        Team team = teamService.getTeamById(teamId);
        return team != null && userId.equals(team.getMentorId());
    }
    
    /**
     * 检查用户是否具有指定角色
     */
    private boolean hasRole(Long userId, String roleName) {
        User user = userService.getUserById(userId);
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains(roleName);
    }

    /**
     * 退出团队（普通成员）
     */
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

    /**
     * 删除团队（仅领导者）
     */
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
    
    /**
     * 上传团队头像
     */
    @PostMapping("/{teamId}/avatar")
    public Result<Map<String, String>> uploadTeamAvatar(
            @PathVariable Long teamId,
            @RequestParam("file") MultipartFile file) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证用户是否为团队成员
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
    
    /**
     * 更新团队信息
     */
    @PutMapping("/{teamId}")
    public Result<Team> updateTeam(@PathVariable Long teamId, @RequestBody Map<String, Object> updates) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证用户是否为团队成员
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
    
    /**
     * 获取团队关联的比赛列表
     */
    @GetMapping("/{teamId}/competitions")
    public Result<List<Map<String, Object>>> getTeamCompetitions(@PathVariable Long teamId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证用户是否为团队成员
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
    
    /**
     * 添加团队关联比赛
     */
    @PostMapping("/{teamId}/competitions/{competitionId}")
    public Result<Void> addTeamCompetition(
            @PathVariable Long teamId, 
            @PathVariable Long competitionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证用户是否为团队领导者
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
    
    /**
     * 移除团队关联比赛
     */
    @DeleteMapping("/{teamId}/competitions/{competitionId}")
    public Result<Void> removeTeamCompetition(
            @PathVariable Long teamId, 
            @PathVariable Long competitionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证用户是否为团队领导者
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
    
    /**
     * 检查用户是否为团队领导者
     */
    private boolean isTeamLeader(Long teamId, Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("user_id", userId);
        wrapper.in("role", "LEADER", "OWNER", "ADMIN");
        return teamMemberMapper.selectCount(wrapper) > 0;
    }
}
