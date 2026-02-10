package com.teamup.server.modules.activity.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.activity.service.ActivityService;
import com.teamup.server.modules.activity.vo.ActivityVO;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动记录控制器
 */
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final TeamService teamService;
    private final TeamMemberMapper teamMemberMapper;

    /**
     * 获取团队活动记录
     * @param teamId 团队ID
     * @param limit 返回数量限制，默认10条
     * @return 活动记录列表
     */
    @GetMapping("/{teamId}/activities")
    public Result<List<ActivityVO>> getTeamActivities(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "10") Integer limit) {
        
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
        
        // 获取活动记录
        List<ActivityVO> activities = activityService.getRecentActivities(teamId, limit);
        return Result.success(activities);
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
}
