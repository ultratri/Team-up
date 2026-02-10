package com.teamup.server.modules.user.filter;

import com.teamup.server.modules.user.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器 - 优化版本，使用缓存减少数据库查询
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final CacheManager cacheManager;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // 从请求头获取Token
            String token = getTokenFromRequest(request);
            
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                // 从Token中获取用户名
                String username = jwtUtil.getUsernameFromToken(token);
                
                // 尝试从缓存获取用户详情，减少数据库查询
                UserDetails userDetails = getUserDetailsFromCache(username);
                if (userDetails == null) {
                    userDetails = userDetailsService.loadUserByUsername(username);
                    cacheUserDetails(username, userDetails);
                }
                
                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // 设置到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("无法设置用户认证: " + e.getMessage(), e);
            // 如果 token 验证失败，继续执行，让 Spring Security 处理未认证请求
            // 这样对于需要认证的接口会返回 401，而不是 403
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * 从缓存获取用户详情
     */
    private UserDetails getUserDetailsFromCache(String username) {
        try {
            Cache cache = cacheManager.getCache("userDetails");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(username);
                if (wrapper != null) {
                    return (UserDetails) wrapper.get();
                }
            }
        } catch (Exception e) {
            logger.warn("从缓存获取用户详情失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 缓存用户详情（5分钟）
     */
    private void cacheUserDetails(String username, UserDetails userDetails) {
        try {
            Cache cache = cacheManager.getCache("userDetails");
            if (cache != null) {
                cache.put(username, userDetails);
            }
        } catch (Exception e) {
            logger.warn("缓存用户详情失败: " + e.getMessage());
        }
    }
}

