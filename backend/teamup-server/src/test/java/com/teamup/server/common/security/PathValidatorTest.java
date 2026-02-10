package com.teamup.server.common.security;

import com.teamup.server.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PathValidator 单元测试
 * 
 * 测试路径遍历攻击防护功能
 * 
 * Validates: Requirements 10.6
 * 
 * Feature: team-features-implementation
 */
public class PathValidatorTest {

    private PathValidator pathValidator;

    @BeforeEach
    void setUp() {
        pathValidator = new PathValidator();
    }

    /**
     * Example 15: 路径遍历攻击防护
     * 
     * 场景：文件路径包含 "../" 等路径遍历字符
     * 期望：抛出 BusinessException
     * 
     * Validates: Requirements 10.6
     */
    @Test
    void testPathTraversalAttackWithDoubleDot() {
        // 测试 "../" 路径遍历
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("../etc/passwd"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("files/../../../etc/passwd"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("files/../../secret.txt"));
        
        // 测试 URL 编码的路径遍历
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("%2e%2e/etc/passwd"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("files/%2e%2e/%2e%2e/secret.txt"));
        
        // 测试反斜杠路径遍历
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("..\\windows\\system32"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("files\\..\\..\\secret.txt"));
    }

    @Test
    void testPathTraversalAttackWithAbsolutePath() {
        // 测试绝对路径
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("/etc/passwd"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("/var/log/secret.log"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("C:\\Windows\\System32"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("C:/Windows/System32"));
    }

    @Test
    void testPathTraversalAttackWithNullByte() {
        // 测试空字节注入
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file.txt\0.jpg"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file\0/etc/passwd"));
    }

    @Test
    void testPathTraversalAttackWithDangerousCharacters() {
        // 测试危险字符 (< > : " | ? *)
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file<test"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file>test"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file:test"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file\"test"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file|test"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file?test"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("file*test"));
    }

    @Test
    void testValidPaths() {
        // 测试合法路径
        assertDoesNotThrow(() -> pathValidator.validateFilePath("document.pdf"));
        assertDoesNotThrow(() -> pathValidator.validateFilePath("folder/document.pdf"));
        assertDoesNotThrow(() -> pathValidator.validateFilePath("folder/subfolder/document.pdf"));
        assertDoesNotThrow(() -> pathValidator.validateFilePath("my-file_2024.txt"));
        assertDoesNotThrow(() -> pathValidator.validateFilePath("文件名.docx"));
    }

    @Test
    void testNullAndEmptyPaths() {
        // 测试 null 和空路径
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath(null));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath(""));
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("   "));
    }

    @Test
    void testSafeJoinPath() {
        // 测试安全路径拼接
        assertDoesNotThrow(() -> pathValidator.safeJoinPath("base", "file.txt"));
        assertDoesNotThrow(() -> pathValidator.safeJoinPath("base", "folder/file.txt"));
        
        // 测试路径遍历攻击在拼接时被阻止
        assertThrows(BusinessException.class, () -> 
            pathValidator.safeJoinPath("base", "../etc/passwd")
        );
        
        assertThrows(BusinessException.class, () -> 
            pathValidator.safeJoinPath("base", "/etc/passwd")
        );
    }

    @Test
    void testPathNormalization() {
        // 测试路径规范化 - 这些路径在规范化后会被拒绝
        assertThrows(BusinessException.class, () -> pathValidator.validateFilePath("folder/../file.txt"));
    }

    @Test
    void testValidateFileName() {
        // 测试合法文件名
        assertDoesNotThrow(() -> pathValidator.validateFileName("document.pdf"));
        assertDoesNotThrow(() -> pathValidator.validateFileName("my-file_2024.txt"));
        assertDoesNotThrow(() -> pathValidator.validateFileName("文件名.docx"));
        
        // 测试非法文件名
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file/name.txt"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file\\name.txt"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("."));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName(".."));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file<name"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file>name"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file:name"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file\"name"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file|name"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file?name"));
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName("file*name"));
        
        // 测试文件名长度
        String longFileName = "a".repeat(256);
        assertThrows(BusinessException.class, () -> pathValidator.validateFileName(longFileName));
    }

    @Test
    void testSecurityLogging() {
        // 测试安全日志记录（不抛出异常）
        assertDoesNotThrow(() -> 
            pathValidator.logSecurityEvent("DELETE", "file.txt", 1L, "Unauthorized access")
        );
    }
}
