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
import com.teamup.server.modules.user.security.UserContext;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;

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
    public Result<Team> getTeam(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        if (team == null) {
            return Result.error(404, "团队不存在");
        }
        return Result.success(team);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Team>> getUserTeams(@PathVariable Long userId) {
        return Result.success(teamService.getUserTeams(userId));
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
    @GetMapping("/{teamId}/statistics")
    public Result<TeamStatisticsVO> getTeamStatistics(@PathVariable Long teamId) {
        // 验证团队是否存在
        Team team = teamService.getTeamById(teamId);
        if (team == null) {
            return Result.error(404, "团队不存在");
        }
        
        // 验证用户是否为团队成员
        Long currentUserId = UserContext.getCurrentUserId();
        if (!isTeamMember(teamId, currentUserId)) {
            return Result.error(403, "无权限访问该团队数据");
        }
        
        // 计算并返回统计数据
        TeamStatisticsVO statistics = statisticsService.calculateStatistics(teamId);
        return Result.success(statistics);
    }
    
    /**
     * 检查用户是否为团队成员
     */
    private boolean isTeamMember(Long teamId, Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("user_id", userId);
        return teamMemberMapper.selectCount(wrapper) > 0;
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
}
