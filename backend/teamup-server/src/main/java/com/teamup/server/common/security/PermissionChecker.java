package com.teamup.server.common.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.file.entity.FileEntity;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 权限检查工具类
 * 提供各种权限验证方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final TeamMemberMapper teamMemberMapper;

    /**
     * 验证用户是否为团队成员
     */
    public boolean isTeamMember(Long teamId, Long userId) {
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", teamId)
                    .eq("user_id", userId);
        
        Long count = teamMemberMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    /**
     * 验证用户是否为团队成员，不是则抛出异常
     */
    public void requireTeamMember(Long teamId, Long userId) {
        if (!isTeamMember(teamId, userId)) {
            log.warn("用户 {} 不是团队 {} 的成员", userId, teamId);
            throw new BusinessException("无权限访问该团队资源");
        }
    }

    /**
     * 获取团队成员信息
     */
    public TeamMember getTeamMember(Long teamId, Long userId) {
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", teamId)
                    .eq("user_id", userId);
        
        return teamMemberMapper.selectOne(queryWrapper);
    }

    /**
     * 验证用户是否为团队管理者
     */
    public boolean isTeamLeader(Long teamId, Long userId) {
        TeamMember member = getTeamMember(teamId, userId);
        return member != null && "LEADER".equals(member.getRole());
    }

    /**
     * 验证用户是否为团队管理者，不是则抛出异常
     */
    public void requireTeamLeader(Long teamId, Long userId) {
        if (!isTeamLeader(teamId, userId)) {
            log.warn("用户 {} 不是团队 {} 的管理者", userId, teamId);
            throw new BusinessException("需要团队管理者权限");
        }
    }

    /**
     * 验证用户是否有删除文件的权限
     * 文件上传者或团队管理者可以删除文件
     */
    public boolean hasFileDeletePermission(FileEntity file, Long userId) {
        // 文件上传者可以删除
        if (file.getUploaderId().equals(userId)) {
            return true;
        }
        
        // 团队管理者可以删除
        return isTeamLeader(file.getTeamId(), userId);
    }

    /**
     * 验证用户是否有删除文件的权限，没有则抛出异常
     */
    public void requireFileDeletePermission(FileEntity file, Long userId) {
        if (!hasFileDeletePermission(file, userId)) {
            log.warn("用户 {} 无权限删除文件 {}", userId, file.getId());
            throw new BusinessException("无权限删除该文件");
        }
    }

    /**
     * 验证当前登录用户是否为团队成员
     */
    public void requireCurrentUserIsTeamMember(Long teamId) {
        Long userId = UserContext.getCurrentUserId();
        requireTeamMember(teamId, userId);
    }

    /**
     * 验证当前登录用户是否为团队管理者
     */
    public void requireCurrentUserIsTeamLeader(Long teamId) {
        Long userId = UserContext.getCurrentUserId();
        requireTeamLeader(teamId, userId);
    }
}
