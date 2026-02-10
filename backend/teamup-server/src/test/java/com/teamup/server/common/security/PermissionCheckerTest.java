package com.teamup.server.common.security;

import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.file.entity.FileEntity;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PermissionChecker 单元测试
 * 
 * 测试未认证访问的错误处理和权限验证
 * 
 * Validates: Requirements 10.1, 10.2, 10.3, 10.6
 * 
 * Feature: team-features-implementation
 */
@ExtendWith(MockitoExtension.class)
public class PermissionCheckerTest {

    @Mock
    private TeamMemberMapper teamMemberMapper;

    private PermissionChecker permissionChecker;

    @BeforeEach
    void setUp() {
        permissionChecker = new PermissionChecker(teamMemberMapper);
    }

    /**
     * 测试团队成员资格验证 - 是成员
     * 
     * 场景：验证用户是否为团队成员
     * 期望：返回 true
     * 
     * Validates: Requirements 10.2
     */
    @Test
    void testIsTeamMember_IsMember() {
        when(teamMemberMapper.selectCount(any())).thenReturn(1L);
        
        assertTrue(permissionChecker.isTeamMember(1L, 1L));
    }

    /**
     * 测试团队成员资格验证 - 不是成员
     * 
     * 场景：验证用户不是团队成员
     * 期望：返回 false
     * 
     * Validates: Requirements 10.2
     */
    @Test
    void testIsTeamMember_NotMember() {
        when(teamMemberMapper.selectCount(any())).thenReturn(0L);
        
        assertFalse(permissionChecker.isTeamMember(1L, 2L));
    }

    /**
     * 测试团队成员资格验证 - 抛出异常
     * 
     * 场景：用户不是团队成员，要求抛出异常
     * 期望：抛出 BusinessException
     * 
     * Validates: Requirements 10.2
     */
    @Test
    void testRequireTeamMember_NotMember() {
        when(teamMemberMapper.selectCount(any())).thenReturn(0L);
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            permissionChecker.requireTeamMember(1L, 2L);
        });
        
        assertEquals("无权限访问该团队资源", exception.getMessage());
    }

    /**
     * 测试团队管理者验证 - 是管理者
     * 
     * 场景：验证用户是否为团队管理者
     * 期望：返回 true
     * 
     * Validates: Requirements 10.3
     */
    @Test
    void testIsTeamLeader_IsLeader() {
        TeamMember member = new TeamMember();
        member.setUserId(1L);
        member.setTeamId(1L);
        member.setRole("LEADER");
        
        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        
        assertTrue(permissionChecker.isTeamLeader(1L, 1L));
    }

    /**
     * 测试团队管理者验证 - 不是管理者
     * 
     * 场景：验证用户不是团队管理者
     * 期望：返回 false
     * 
     * Validates: Requirements 10.3
     */
    @Test
    void testIsTeamLeader_NotLeader() {
        TeamMember member = new TeamMember();
        member.setUserId(1L);
        member.setTeamId(1L);
        member.setRole("MEMBER");
        
        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        
        assertFalse(permissionChecker.isTeamLeader(1L, 1L));
    }

    /**
     * 测试团队管理者验证 - 抛出异常
     * 
     * 场景：用户不是团队管理者，要求抛出异常
     * 期望：抛出 BusinessException
     * 
     * Validates: Requirements 10.3
     */
    @Test
    void testRequireTeamLeader_NotLeader() {
        TeamMember member = new TeamMember();
        member.setUserId(1L);
        member.setTeamId(1L);
        member.setRole("MEMBER");
        
        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            permissionChecker.requireTeamLeader(1L, 1L);
        });
        
        assertEquals("需要团队管理者权限", exception.getMessage());
    }

    /**
     * 测试文件删除权限验证 - 文件上传者
     * 
     * 场景：验证文件上传者是否有权限删除文件
     * 期望：返回 true
     * 
     * Validates: Requirements 10.3
     */
    @Test
    void testHasFileDeletePermission_Uploader() {
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setTeamId(1L);
        file.setUploaderId(1L);
        
        assertTrue(permissionChecker.hasFileDeletePermission(file, 1L));
    }

    /**
     * 测试文件删除权限验证 - 团队管理者
     * 
     * 场景：验证团队管理者是否有权限删除文件
     * 期望：返回 true
     * 
     * Validates: Requirements 10.3
     */
    @Test
    void testHasFileDeletePermission_TeamLeader() {
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setTeamId(1L);
        file.setUploaderId(2L);
        
        TeamMember member = new TeamMember();
        member.setUserId(1L);
        member.setTeamId(1L);
        member.setRole("LEADER");
        
        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        
        assertTrue(permissionChecker.hasFileDeletePermission(file, 1L));
    }

    /**
     * 测试文件删除权限验证 - 无权限
     * 
     * 场景：验证普通成员是否有权限删除他人文件
     * 期望：返回 false
     * 
     * Validates: Requirements 10.3
     */
    @Test
    void testHasFileDeletePermission_NoPermission() {
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setTeamId(1L);
        file.setUploaderId(2L);
        
        TeamMember member = new TeamMember();
        member.setUserId(3L);
        member.setTeamId(1L);
        member.setRole("MEMBER");
        
        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        
        assertFalse(permissionChecker.hasFileDeletePermission(file, 3L));
    }

    /**
     * 测试文件删除权限验证 - 抛出异常
     * 
     * 场景：用户无权限删除文件，要求抛出异常
     * 期望：抛出 BusinessException
     * 
     * Validates: Requirements 10.3
     */
    @Test
    void testRequireFileDeletePermission_NoPermission() {
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setTeamId(1L);
        file.setUploaderId(2L);
        
        TeamMember member = new TeamMember();
        member.setUserId(3L);
        member.setTeamId(1L);
        member.setRole("MEMBER");
        
        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            permissionChecker.requireFileDeletePermission(file, 3L);
        });
        
        assertEquals("无权限删除该文件", exception.getMessage());
    }

    /**
     * 测试权限检查器实例化
     * 
     * 验证权限检查器可以正确实例化
     */
    @Test
    void testPermissionCheckerInstantiation() {
        assertNotNull(permissionChecker);
        assertTrue(permissionChecker instanceof PermissionChecker);
    }
}
