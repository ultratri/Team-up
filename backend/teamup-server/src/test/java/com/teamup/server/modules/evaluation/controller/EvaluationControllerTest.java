package com.teamup.server.modules.evaluation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.evaluation.dto.EvaluationDTO;
import com.teamup.server.modules.evaluation.service.EvaluationService;
import com.teamup.server.modules.evaluation.vo.EvaluationVO;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 评价控制器单元测试
 * Validates: Requirements 6.5, 6.6, 6.8
 */
@ExtendWith(MockitoExtension.class)
class EvaluationControllerTest {

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private TeamService teamService;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @InjectMocks
    private EvaluationController evaluationController;

    private Long testTeamId = 1L;
    private Long testUserId = 100L;
    private Long testEvaluatedId = 200L;

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
     * Example 9: 无效评价数据的错误处理 - 分数超出范围
     * 场景：提交分数超出 1-5 范围的评价
     * 期望：返回 400 错误和参数验证错误信息
     * Validates: Requirements 6.8
     */
    @Test
    void testSubmitEvaluation_InvalidScoreOutOfRange() {
        // Given: Team exists, user is member, but scores are out of range
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testEvaluatedId);
        dto.setTechContributionScore(6); // Invalid: > 5
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(4);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        doThrow(new BusinessException("技术贡献分数必须在1-5之间"))
            .when(evaluationService).submitEvaluation(eq(testTeamId), eq(testUserId), any(EvaluationDTO.class));

        // When: Submit evaluation with invalid score
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return 400 error
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("技术贡献分数必须在1-5之间"));
        
        // Verify service was called
        verify(evaluationService).submitEvaluation(eq(testTeamId), eq(testUserId), any(EvaluationDTO.class));
    }

    /**
     * Example 9: 无效评价数据的错误处理 - 分数为0
     * 场景：提交分数为0的评价
     * 期望：返回 400 错误和参数验证错误信息
     * Validates: Requirements 6.8
     */
    @Test
    void testSubmitEvaluation_InvalidScoreZero() {
        // Given: Team exists, user is member, but score is 0
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testEvaluatedId);
        dto.setTechContributionScore(0); // Invalid: < 1
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(4);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        doThrow(new BusinessException("技术贡献分数必须在1-5之间"))
            .when(evaluationService).submitEvaluation(eq(testTeamId), eq(testUserId), any(EvaluationDTO.class));

        // When: Submit evaluation with invalid score
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return 400 error
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("技术贡献分数必须在1-5之间"));
    }

    /**
     * Example 9: 无效评价数据的错误处理 - 被评价者不是团队成员
     * 场景：提交评价时被评价者不是团队成员
     * 期望：返回 400 错误和错误信息
     * Validates: Requirements 6.8
     */
    @Test
    void testSubmitEvaluation_EvaluatedNotTeamMember() {
        // Given: Team exists, evaluator is member, but evaluated is not
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testEvaluatedId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(5);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        // First call: check if evaluator is member (returns 1)
        // Second call: check if evaluated is member (returns 0)
        when(teamMemberMapper.selectCount(any(QueryWrapper.class)))
            .thenReturn(1L)  // Evaluator is member
            .thenReturn(0L); // Evaluated is NOT member

        // When: Submit evaluation for non-member
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return 400 error
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("被评价者不是该团队成员", result.getMessage());
        
        // Service should not be called
        verify(evaluationService, never()).submitEvaluation(any(), any(), any());
    }

    /**
     * Test: 重复评价的错误处理
     * 场景：用户尝试对同一成员重复评价
     * 期望：返回 400 错误和 "已经评价过该成员" 消息
     * Validates: Requirements 6.5
     */
    @Test
    void testSubmitEvaluation_DuplicateEvaluation() {
        // Given: Team exists, user is member, but already evaluated
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testEvaluatedId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(5);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        doThrow(new BusinessException("已经评价过该成员"))
            .when(evaluationService).submitEvaluation(eq(testTeamId), eq(testUserId), any(EvaluationDTO.class));

        // When: Submit duplicate evaluation
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return 400 error
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("已经评价过该成员", result.getMessage());
        
        // Verify service was called
        verify(evaluationService).submitEvaluation(eq(testTeamId), eq(testUserId), any(EvaluationDTO.class));
    }

    /**
     * Test: 自我评价的错误处理
     * 场景：用户尝试评价自己
     * 期望：返回 400 错误和 "不能评价自己" 消息
     * Validates: Requirements 6.6
     */
    @Test
    void testSubmitEvaluation_SelfEvaluation() {
        // Given: Team exists, user is member, but trying to evaluate self
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testUserId); // Same as evaluator
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(5);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // When: Submit self-evaluation
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return 400 error
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("不能评价自己", result.getMessage());
        
        // Service should not be called
        verify(evaluationService, never()).submitEvaluation(any(), any(), any());
    }

    /**
     * Test: 成功提交评价
     * 场景：用户提交有效的评价数据
     * 期望：返回 200 成功响应
     * Validates: Requirements 6.8
     */
    @Test
    void testSubmitEvaluation_Success() {
        // Given: Valid team, member, and evaluation data
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testEvaluatedId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(5);
        dto.setComment("Great work!");
        dto.setIsAnonymous(false);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        doNothing().when(evaluationService).submitEvaluation(eq(testTeamId), eq(testUserId), any(EvaluationDTO.class));

        // When: Submit valid evaluation
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return success
        assertNotNull(result);
        assertEquals(200, result.getCode());
        
        // Verify service was called
        verify(evaluationService).submitEvaluation(eq(testTeamId), eq(testUserId), any(EvaluationDTO.class));
    }

    /**
     * Test: 团队不存在的错误处理
     * 场景：提交评价时团队不存在
     * 期望：返回 404 错误
     * Validates: Requirements 6.8
     */
    @Test
    void testSubmitEvaluation_TeamNotFound() {
        // Given: Team does not exist
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testEvaluatedId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(5);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(null);

        // When: Submit evaluation for non-existent team
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return 404 error
        assertNotNull(result);
        assertEquals(404, result.getCode());
        assertEquals("团队不存在", result.getMessage());
        
        // Service should not be called
        verify(evaluationService, never()).submitEvaluation(any(), any(), any());
    }

    /**
     * Test: 无权限访问团队
     * 场景：非团队成员尝试提交评价
     * 期望：返回 403 错误
     * Validates: Requirements 6.8
     */
    @Test
    void testSubmitEvaluation_NoPermission() {
        // Given: Team exists but user is not a member
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(testEvaluatedId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(5);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        // When: Non-member tries to submit evaluation
        Result<Void> result = evaluationController.submitEvaluation(testTeamId, dto);

        // Then: Should return 403 error
        assertNotNull(result);
        assertEquals(403, result.getCode());
        assertEquals("无权限访问该团队", result.getMessage());
        
        // Service should not be called
        verify(evaluationService, never()).submitEvaluation(any(), any(), any());
    }

    /**
     * Test: 获取评价列表 - 成功
     * 场景：团队成员获取评价列表
     * 期望：返回评价列表
     */
    @Test
    void testGetTeamEvaluations_Success() {
        // Given: Valid team and member
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        List<EvaluationVO> evaluations = new ArrayList<>();
        EvaluationVO vo = new EvaluationVO();
        vo.setId(1L);
        vo.setEvaluatorId(testUserId);
        vo.setEvaluatedId(testEvaluatedId);
        vo.setTechContributionScore(4);
        vo.setCollaborationScore(3);
        vo.setTaskCompletionScore(5);
        evaluations.add(vo);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        when(evaluationService.getEvaluations(testTeamId)).thenReturn(evaluations);

        // When: Get evaluations
        Result<List<EvaluationVO>> result = evaluationController.getTeamEvaluations(testTeamId);

        // Then: Should return success with data
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        
        // Verify service was called
        verify(evaluationService).getEvaluations(testTeamId);
    }

    /**
     * Test: 获取评价列表 - 团队不存在
     * 场景：获取不存在团队的评价列表
     * 期望：返回 404 错误
     */
    @Test
    void testGetTeamEvaluations_TeamNotFound() {
        // Given: Team does not exist
        when(teamService.getTeamById(testTeamId)).thenReturn(null);

        // When: Get evaluations for non-existent team
        Result<List<EvaluationVO>> result = evaluationController.getTeamEvaluations(testTeamId);

        // Then: Should return 404 error
        assertNotNull(result);
        assertEquals(404, result.getCode());
        assertEquals("团队不存在", result.getMessage());
        
        // Service should not be called
        verify(evaluationService, never()).getEvaluations(any());
    }

    /**
     * Test: 获取评价列表 - 无权限
     * 场景：非团队成员尝试获取评价列表
     * 期望：返回 403 错误
     */
    @Test
    void testGetTeamEvaluations_NoPermission() {
        // Given: Team exists but user is not a member
        Team team = new Team();
        team.setId(testTeamId);
        team.setTeamName("Test Team");
        team.setProjectId(1L);
        
        when(teamService.getTeamById(testTeamId)).thenReturn(team);
        when(teamMemberMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        // When: Non-member tries to get evaluations
        Result<List<EvaluationVO>> result = evaluationController.getTeamEvaluations(testTeamId);

        // Then: Should return 403 error
        assertNotNull(result);
        assertEquals(403, result.getCode());
        assertEquals("无权限访问该团队数据", result.getMessage());
        
        // Service should not be called
        verify(evaluationService, never()).getEvaluations(any());
    }
}
