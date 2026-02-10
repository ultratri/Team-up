package com.teamup.server.common.security;

import com.teamup.server.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 路径验证工具类
 * 防止路径遍历攻击
 */
@Slf4j
@Component
public class PathValidator {

    // 路径遍历攻击模式
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*(\\.\\./|\\.\\.\\\\|%2e%2e/|%2e%2e\\\\).*", Pattern.CASE_INSENSITIVE);
    
    // 危险字符模式
    private static final Pattern DANGEROUS_CHARS_PATTERN = Pattern.compile(".*[<>:\"|?*].*");
    
    // 空字节注入模式
    private static final Pattern NULL_BYTE_PATTERN = Pattern.compile(".*\\x00.*");

    /**
     * 验证文件路径是否安全
     * @param filePath 文件路径
     * @throws BusinessException 如果路径不安全
     */
    public void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }

        // 1. 检查路径遍历攻击
        if (PATH_TRAVERSAL_PATTERN.matcher(filePath).matches()) {
            log.warn("检测到路径遍历攻击尝试: {}", filePath);
            throw new BusinessException("非法的文件路径");
        }

        // 2. 检查危险字符
        if (DANGEROUS_CHARS_PATTERN.matcher(filePath).matches()) {
            log.warn("检测到危险字符: {}", filePath);
            throw new BusinessException("文件路径包含非法字符");
        }

        // 3. 检查空字节注入
        if (NULL_BYTE_PATTERN.matcher(filePath).matches()) {
            log.warn("检测到空字节注入尝试: {}", filePath);
            throw new BusinessException("文件路径包含非法字符");
        }

        // 4. 检查绝对路径
        if (filePath.startsWith("/") || filePath.matches("^[a-zA-Z]:.*")) {
            log.warn("检测到绝对路径: {}", filePath);
            throw new BusinessException("不允许使用绝对路径");
        }

        // 5. 规范化路径并验证
        try {
            Path normalizedPath = Paths.get(filePath).normalize();
            String normalizedStr = normalizedPath.toString();
            
            // 检查规范化后的路径是否包含 ".."
            if (normalizedStr.contains("..")) {
                log.warn("规范化后仍包含路径遍历: {}", normalizedStr);
                throw new BusinessException("非法的文件路径");
            }
        } catch (Exception e) {
            log.warn("路径规范化失败: {}", filePath, e);
            throw new BusinessException("无效的文件路径");
        }
    }

    /**
     * 验证文件名是否安全
     * @param fileName 文件名
     * @throws BusinessException 如果文件名不安全
     */
    public void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        // 1. 检查路径分隔符
        if (fileName.contains("/") || fileName.contains("\\")) {
            log.warn("文件名包含路径分隔符: {}", fileName);
            throw new BusinessException("文件名不能包含路径分隔符");
        }

        // 2. 检查特殊文件名
        if (".".equals(fileName) || "..".equals(fileName)) {
            log.warn("检测到特殊文件名: {}", fileName);
            throw new BusinessException("非法的文件名");
        }

        // 3. 检查危险字符
        if (DANGEROUS_CHARS_PATTERN.matcher(fileName).matches()) {
            log.warn("文件名包含危险字符: {}", fileName);
            throw new BusinessException("文件名包含非法字符");
        }

        // 4. 检查空字节注入
        if (NULL_BYTE_PATTERN.matcher(fileName).matches()) {
            log.warn("检测到空字节注入尝试: {}", fileName);
            throw new BusinessException("文件名包含非法字符");
        }

        // 5. 检查文件名长度
        if (fileName.length() > 255) {
            log.warn("文件名过长: {} 字符", fileName.length());
            throw new BusinessException("文件名过长");
        }
    }

    /**
     * 安全地拼接路径
     * @param basePath 基础路径
     * @param relativePath 相对路径
     * @return 安全的完整路径
     * @throws BusinessException 如果路径不安全
     */
    public Path safeJoinPath(String basePath, String relativePath) {
        validateFilePath(relativePath);
        
        try {
            Path base = Paths.get(basePath).normalize().toAbsolutePath();
            Path resolved = base.resolve(relativePath).normalize();
            
            // 确保解析后的路径仍在基础路径下
            if (!resolved.startsWith(base)) {
                log.warn("路径遍历尝试: base={}, relative={}, resolved={}", 
                        basePath, relativePath, resolved);
                throw new BusinessException("非法的文件路径");
            }
            
            return resolved;
        } catch (Exception e) {
            log.warn("路径拼接失败: base={}, relative={}", basePath, relativePath, e);
            throw new BusinessException("无效的文件路径");
        }
    }

    /**
     * 记录安全日志
     * @param action 操作类型
     * @param path 路径
     * @param userId 用户ID
     * @param reason 原因
     */
    public void logSecurityEvent(String action, String path, Long userId, String reason) {
        log.warn("安全事件 - 操作: {}, 路径: {}, 用户ID: {}, 原因: {}", 
                action, path, userId, reason);
    }
}
