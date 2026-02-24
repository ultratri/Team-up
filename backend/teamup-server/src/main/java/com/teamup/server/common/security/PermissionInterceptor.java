package com.teamup.server.common.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.user.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * 权限验证拦截器 - 优化版本，使用缓存减少数据库查询
 * 验证用户身份、团队成员资格和操作权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final TeamMemberMapper teamMemberMapper;
    private final TeamMapper teamMapper;
    private final CacheManager cacheManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 验证用户身份（JWT Token已在JwtAuthenticationFilter中验证）
        // 直接尝试获取用户ID，如果失败会抛出异常
        Long userId;
        try {
            userId = UserContext.getCurrentUserId();
        } catch (RuntimeException e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
            throw new BusinessException("未认证，请先登录");
        }

        // 2. 验证团队成员资格
        Long teamId = extractTeamId(request);
        if (teamId != null) {
            if (!isTeamMemberCached(teamId, userId)) {
                log.warn("用户 {} 尝试访问非成员团队 {}", userId, teamId);
                throw new BusinessException("无权限访问该团队资源");
            }
        }

        // 3. 验证删除操作权限（在具体的Service层实现）
        // 这里只做基础的团队成员验证，具体的删除权限在FileService等服务中验证

        return true;
    }

    /**
     * 从请求中提取团队ID
     */
    private Long extractTeamId(HttpServletRequest request) {
        // 从路径变量中提取
        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = (Map<String, String>) request
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        
        if (pathVariables != null && pathVariables.containsKey("teamId")) {
            try {
                return Long.parseLong(pathVariables.get("teamId"));
            } catch (NumberFormatException e) {
                log.warn("无效的团队ID格式: {}", pathVariables.get("teamId"));
            }
        }

        // 从查询参数中提取
        String teamIdParam = request.getParameter("teamId");
        if (teamIdParam != null) {
            try {
                return Long.parseLong(teamIdParam);
            } catch (NumberFormatException e) {
                log.warn("无效的团队ID格式: {}", teamIdParam);
            }
        }

        return null;
    }

    /**
     * 验证用户是否为团队成员（带缓存）
     */
    private boolean isTeamMemberCached(Long teamId, Long userId) {
        String cacheKey = "team:" + teamId + ":member:" + userId;
        
        try {
            Cache cache = cacheManager.getCache("teamMembers");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null) {
                    return Boolean.TRUE.equals(wrapper.get());
                }
            }
        } catch (Exception e) {
            log.warn("从缓存获取团队成员信息失败: {}", e.getMessage());
        }
        
        // 缓存未命中，查询数据库
        boolean isMember = isTeamMember(teamId, userId);
        
        // 缓存结果（5分钟）
        try {
            Cache cache = cacheManager.getCache("teamMembers");
            if (cache != null) {
                cache.put(cacheKey, isMember);
            }
        } catch (Exception e) {
            log.warn("缓存团队成员信息失败: {}", e.getMessage());
        }
        
        return isMember;
    }

    /**
     * 验证用户是否为团队成员或导师
     */
    private boolean isTeamMember(Long teamId, Long userId) {
        // 检查是否为团队成员
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", teamId)
                    .eq("user_id", userId);
        
        Long count = teamMemberMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            return true;
        }
        
        // 检查是否为团队导师
        Team team = teamMapper.selectById(teamId);
        return team != null && userId.equals(team.getMentorId());
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
}
