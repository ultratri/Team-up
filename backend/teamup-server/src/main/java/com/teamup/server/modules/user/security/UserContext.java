package com.teamup.server.modules.user.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类
 */
public class UserContext {
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        
        throw new RuntimeException("无法获取用户ID");
    }
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        return authentication.getName();
    }
    
    /**
     * 判断是否已登录
     * 注意：不检查 isAuthenticated()，因为它可能返回 false
     * 只检查 principal 是否为 CustomUserDetails
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        Object principal = authentication.getPrincipal();
        return principal instanceof CustomUserDetails;
    }
    
    /**
     * 判断当前用户是否拥有任意一个指定角色
     */
    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        for (String role : roles) {
            // Spring Security 的角色通常带有 ROLE_ 前缀
            String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (authority.getAuthority().equals(roleWithPrefix) || 
                    authority.getAuthority().equals(role)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
