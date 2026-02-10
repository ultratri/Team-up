package com.teamup.server.common.audit;

import com.teamup.server.modules.user.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志服务
 * 记录所有敏感操作的审计日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 记录文件删除操作
     */
    @Async
    public void logFileDelete(Long fileId, String fileName, String result, String errorMessage) {
        try {
            Long userId = UserContext.getCurrentUserId();
            String username = UserContext.getCurrentUsername();
            
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction("DELETE_FILE");
            auditLog.setResourceType("FILE");
            auditLog.setResourceId(fileId);
            auditLog.setDetails("删除文件: " + fileName);
            auditLog.setResult(result);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // 获取请求信息
            setRequestInfo(auditLog);
            
            auditLogMapper.insert(auditLog);
            
            log.info("审计日志已记录: action=DELETE_FILE, fileId={}, userId={}, result={}", 
                    fileId, userId, result);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录权限修改操作
     */
    @Async
    public void logPermissionChange(Long teamId, Long targetUserId, String oldRole, String newRole, 
                                     String result, String errorMessage) {
        try {
            Long userId = UserContext.getCurrentUserId();
            String username = UserContext.getCurrentUsername();
            
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction("CHANGE_PERMISSION");
            auditLog.setResourceType("TEAM_MEMBER");
            auditLog.setResourceId(targetUserId);
            auditLog.setDetails(String.format("团队 %d: 修改用户 %d 权限从 %s 到 %s", 
                    teamId, targetUserId, oldRole, newRole));
            auditLog.setResult(result);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // 获取请求信息
            setRequestInfo(auditLog);
            
            auditLogMapper.insert(auditLog);
            
            log.info("审计日志已记录: action=CHANGE_PERMISSION, teamId={}, targetUserId={}, userId={}, result={}", 
                    teamId, targetUserId, userId, result);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录成员删除操作
     */
    @Async
    public void logMemberRemove(Long teamId, Long removedUserId, String removedUsername, 
                                String result, String errorMessage) {
        try {
            Long userId = UserContext.getCurrentUserId();
            String username = UserContext.getCurrentUsername();
            
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction("REMOVE_MEMBER");
            auditLog.setResourceType("TEAM_MEMBER");
            auditLog.setResourceId(removedUserId);
            auditLog.setDetails(String.format("从团队 %d 移除成员: %s (ID: %d)", 
                    teamId, removedUsername, removedUserId));
            auditLog.setResult(result);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // 获取请求信息
            setRequestInfo(auditLog);
            
            auditLogMapper.insert(auditLog);
            
            log.info("审计日志已记录: action=REMOVE_MEMBER, teamId={}, removedUserId={}, userId={}, result={}", 
                    teamId, removedUserId, userId, result);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录文件夹删除操作
     */
    @Async
    public void logFolderDelete(Long folderId, String folderName, String result, String errorMessage) {
        try {
            Long userId = UserContext.getCurrentUserId();
            String username = UserContext.getCurrentUsername();
            
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction("DELETE_FOLDER");
            auditLog.setResourceType("FOLDER");
            auditLog.setResourceId(folderId);
            auditLog.setDetails("删除文件夹: " + folderName);
            auditLog.setResult(result);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // 获取请求信息
            setRequestInfo(auditLog);
            
            auditLogMapper.insert(auditLog);
            
            log.info("审计日志已记录: action=DELETE_FOLDER, folderId={}, userId={}, result={}", 
                    folderId, userId, result);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录通用敏感操作（只记录管理员操作）
     */
    @Async
    public void logSensitiveOperation(String action, String resourceType, Long resourceId, 
                                      String details, String result, String errorMessage) {
        try {
            Long userId = null;
            String username = null;

            // @Async 场景下可能拿不到 SecurityContext（线程切换），这里必须降级处理
            if (UserContext.isAuthenticated()) {
                try {
                    userId = UserContext.getCurrentUserId();
                } catch (Exception ignored) {
                    userId = null;
                }
                username = UserContext.getCurrentUsername();
                
                // 只记录管理员操作
                if (!UserContext.hasAnyRole("PLATFORM_ADMIN")) {
                    log.debug("跳过非管理员操作的审计日志: action={}, userId={}", action, userId);
                    return;
                }
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setDetails(details);
            auditLog.setResult(result);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setCreatedAt(LocalDateTime.now());

            // 获取请求信息
            setRequestInfo(auditLog);

            auditLogMapper.insert(auditLog);

            log.info("审计日志已记录: action={}, resourceType={}, resourceId={}, userId={}, result={}", 
                    action, resourceType, resourceId, userId, result);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 设置请求信息（IP地址和User Agent）
     */
    private void setRequestInfo(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // 获取IP地址
                String ipAddress = getClientIpAddress(request);
                auditLog.setIpAddress(ipAddress);
                
                // 获取User Agent
                String userAgent = request.getHeader("User-Agent");
                auditLog.setUserAgent(userAgent);
            }
        } catch (Exception e) {
            log.warn("获取请求信息失败", e);
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
