package com.teamup.server.modules.team.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.team.service.TeamStatisticsService;
import com.teamup.server.modules.team.vo.TeamStatisticsVO;
import com.teamup.server.modules.user.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 团队统计控制器单元测试
 * Validates: Requirements 1.7
 */
@ExtendWith(MockitoExtension.class)
class TeamStatisticsControllerTest {

    @Mock
    private TeamService teamService;

    @Mock
    private TeamStatisticsService statisticsService;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @InjectMocks
    private TeamController teamController;

    private Long testTeamId = 1L;
    private Long testUserId = 100L;

    @BeforeEach
    void setUp() {
        // Setup security context with authenticated user
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getId()).thenReturn(testUserId);
        lenient().when(userDetails.getUsername()).thenReturn("testuser");
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userDetails, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Example 1: 团队不存在错误处理
     * 场景：请求不存在的团队 ID 的统计数据
     * 期望：返回 404 错误和 "团队不存在" 消息
     * Validates: Requirements 1.7
     */
    @Test
    void testGetStatistics_TeamNotFound() {
        // Given: Team does not exist
        when(teamService.getTeamById(testTeamId)).thenReturn(null);

        // When: Request statistics for non-existent team
        Result<TeamStatisticsVO> result = teamController.getTeamStatistics(testTeamId);

        // Then: Should return 404 error
        assertNotNull(result);
        assertEquals(404, result.getCode());
        assertEquals("团队不存在", result.getMessage());
        assertNull(result.getData());
        
        // Verify service was called
        verify(teamService).getTeamById(testTeamId);
        // Statistics service should not be called
        verify(statisticsService, never()).calculateStatistics(any());
    }

    /**
     * Example 2: 无权限访问团队统计数据
     * 场景：非团队成员尝试访问团队统计数据
     * 期望：返回 403 错误和 "无权限访问该团队数据" 消息
     * Validates: Requirements 1.7
     */
    @Test
    void testGetStatistics_NoPermission() {
        // Given: Team exists but user is not a member
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setCreatedAt(LocalDateTime.now());
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        // When: Non-member requests statistics
        Result<TeamStatisticsVO> result = teamController.getTeamStatistics(testTeamId);

        // Then: Should return 403 error
        assertNotNull(result);
        assertEquals(403, result.getCode());
        assertEquals("无权限访问该团队数据", result.getMessage());
        assertNull(result.getData());
        
        // Verify services were called
        verify(teamService).getTeamById(testTeamId);
        verify(teamMemberMapper).selectCount(any(QueryWrapper.class));
        // Statistics service should not be called
        verify(statisticsService, never()).calculateStatistics(any());
    }

    /**
     * Example 3: 正常情况的响应格式
     * 场景：团队成员请求团队统计数据
     * 期望：返回 200 成功响应和统计数据
     * Validates: Requirements 1.7
     */
    @Test
    void testGetStatistics_Success() {
        // Given: Team exists and user is a member
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setCreatedAt(LocalDateTime.now());
        
        TeamStatisticsVO statistics = new TeamStatisticsVO();
        statistics.setTaskCompletionRate(75);
        statistics.setActiveDays(30);
        statistics.setMessageCount(150);
        statistics.setFileCount(25);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        when(statisticsService.calculateStatistics(testTeamId)).thenReturn(statistics);

        // When: Team member requests statistics
        Result<TeamStatisticsVO> result = teamController.getTeamStatistics(testTeamId);

        // Then: Should return success with statistics data
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertNotNull(result.getData());
        
        // Verify statistics data
        TeamStatisticsVO data = result.getData();
        assertEquals(75, data.getTaskCompletionRate());
        assertEquals(30, data.getActiveDays());
        assertEquals(150, data.getMessageCount());
        assertEquals(25, data.getFileCount());
        
        // Verify all services were called
        verify(teamService).getTeamById(testTeamId);
        verify(teamMemberMapper).selectCount(any(QueryWrapper.class));
        verify(statisticsService).calculateStatistics(testTeamId);
    }

    /**
     * Test: 验证响应格式包含所有必需字段
     * Validates: Requirements 1.7
     */
    @Test
    void testGetStatistics_ResponseFormat() {
        // Given: Valid team and member
        Team team = new Team();
        team.setId(testTeamId);
        
        TeamStatisticsVO statistics = new TeamStatisticsVO();
        statistics.setTaskCompletionRate(0);
        statistics.setActiveDays(1);
        statistics.setMessageCount(0);
        statistics.setFileCount(0);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        when(statisticsService.calculateStatistics(testTeamId)).thenReturn(statistics);

        // When: Request statistics
        Result<TeamStatisticsVO> result = teamController.getTeamStatistics(testTeamId);

        // Then: Response should have proper format
        assertNotNull(result);
        assertNotNull(result.getCode(), "Response should have code");
        assertNotNull(result.getMessage(), "Response should have message");
        assertNotNull(result.getData(), "Response should have data");
        
        // Verify data structure
        TeamStatisticsVO data = result.getData();
        assertNotNull(data.getTaskCompletionRate(), "Should have task completion rate");
        assertNotNull(data.getActiveDays(), "Should have active days");
        assertNotNull(data.getMessageCount(), "Should have message count");
        assertNotNull(data.getFileCount(), "Should have file count");
    }
}
