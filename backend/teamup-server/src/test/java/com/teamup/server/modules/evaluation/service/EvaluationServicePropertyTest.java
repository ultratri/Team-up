package com.teamup.server.modules.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.evaluation.dto.EvaluationDTO;
import com.teamup.server.modules.evaluation.entity.Evaluation;
import com.teamup.server.modules.evaluation.mapper.EvaluationMapper;
import com.teamup.server.modules.evaluation.service.impl.EvaluationServiceImpl;
import com.teamup.server.modules.evaluation.vo.EvaluationVO;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 评价服务属性测试
 * Feature: team-features-implementation
 */
public class EvaluationServicePropertyTest {

    private EvaluationMapper evaluationMapper;
    private TeamMapper teamMapper;
    private UserMapper userMapper;
    private EvaluationServiceImpl evaluationService;

    private void setUp() {
        evaluationMapper = mock(EvaluationMapper.class);
        teamMapper = mock(TeamMapper.class);
        userMapper = mock(UserMapper.class);
        evaluationService = new EvaluationServiceImpl();
        
        // Use reflection to inject mocked dependencies
        try {
            java.lang.reflect.Field evaluationMapperField = EvaluationServiceImpl.class.getDeclaredField("evaluationMapper");
            evaluationMapperField.setAccessible(true);
            evaluationMapperField.set(evaluationService, evaluationMapper);
            
            java.lang.reflect.Field teamMapperField = EvaluationServiceImpl.class.getDeclaredField("teamMapper");
            teamMapperField.setAccessible(true);
            teamMapperField.set(evaluationService, teamMapper);
            
            java.lang.reflect.Field userMapperField = EvaluationServiceImpl.class.getDeclaredField("userMapper");
            userMapperField.setAccessible(true);
            userMapperField.set(evaluationService, userMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocked dependencies", e);
        }
    }

    /**
     * Property 17: 评价分数范围验证
     * For any 评价提交，技术贡献分数、协作能力分数和任务完成分数都应该在 1-5 的范围内，否则应该被拒绝
     * Validates: Requirements 6.1
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 17: 评价分数范围验证")
    void evaluationScoresShouldBeInValidRange(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long evaluatorId,
            @ForAll("userIds") Long evaluatedId,
            @ForAll("invalidScores") Integer invalidScore) {
        
        // Setup
        setUp();
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(1L);
        when(teamMapper.selectById(teamId)).thenReturn(team);
        
        User evaluatedUser = new User();
        evaluatedUser.setId(evaluatedId);
        evaluatedUser.setUsername("evaluated_user");
        when(userMapper.selectById(evaluatedId)).thenReturn(evaluatedUser);
        
        when(evaluationMapper.countByProjectAndEvaluatorAndEvaluated(anyLong(), anyLong(), anyLong())).thenReturn(0);
        
        // Create DTO with invalid score
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(evaluatedId);
        dto.setTechContributionScore(invalidScore);
        dto.setCollaborationScore(3);
        dto.setTaskCompletionScore(3);
        
        // Execute & Verify: Should throw BusinessException for invalid score
        if (evaluatorId.equals(evaluatedId)) {
            // Skip if evaluator and evaluated are the same (self-evaluation)
            return;
        }
        
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> evaluationService.submitEvaluation(teamId, evaluatorId, dto),
            "Should reject evaluation with invalid score: " + invalidScore
        );
        
        assertTrue(
            exception.getMessage().contains("必须在1-5之间"),
            "Error message should indicate score range requirement"
        );
    }

    /**
     * Property 18: 评价数据完整性
     * For any 成功提交的评价，应该包含技术贡献分数、协作能力分数和任务完成分数这三个必需字段
     * Validates: Requirements 6.2
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 18: 评价数据完整性")
    void submittedEvaluationShouldContainAllRequiredScores(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long evaluatorId,
            @ForAll("differentUserIds") Long evaluatedId,
            @ForAll("validScores") Integer techScore,
            @ForAll("validScores") Integer collabScore,
            @ForAll("validScores") Integer taskScore) {
        
        // Skip if evaluator and evaluated are the same
        if (evaluatorId.equals(evaluatedId)) {
            return;
        }
        
        // Setup
        setUp();
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(1L);
        when(teamMapper.selectById(teamId)).thenReturn(team);
        
        User evaluatedUser = new User();
        evaluatedUser.setId(evaluatedId);
        evaluatedUser.setUsername("evaluated_user");
        when(userMapper.selectById(evaluatedId)).thenReturn(evaluatedUser);
        
        when(evaluationMapper.countByProjectAndEvaluatorAndEvaluated(anyLong(), anyLong(), anyLong())).thenReturn(0);
        
        // Track captured evaluation
        final Evaluation[] capturedEvaluation = {null};
        when(evaluationMapper.insert(any(Evaluation.class))).thenAnswer(invocation -> {
            capturedEvaluation[0] = invocation.getArgument(0);
            return 1;
        });
        
        // Create DTO with all required scores
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(evaluatedId);
        dto.setTechContributionScore(techScore);
        dto.setCollaborationScore(collabScore);
        dto.setTaskCompletionScore(taskScore);
        
        // Execute
        evaluationService.submitEvaluation(teamId, evaluatorId, dto);
        
        // Verify: All three scores should be present
        assertNotNull(capturedEvaluation[0], "Evaluation should be created");
        assertNotNull(capturedEvaluation[0].getTechContributionScore(), "Tech contribution score should not be null");
        assertNotNull(capturedEvaluation[0].getCollaborationScore(), "Collaboration score should not be null");
        assertNotNull(capturedEvaluation[0].getTaskCompletionScore(), "Task completion score should not be null");
        
        assertEquals(techScore, capturedEvaluation[0].getTechContributionScore());
        assertEquals(collabScore, capturedEvaluation[0].getCollaborationScore());
        assertEquals(taskScore, capturedEvaluation[0].getTaskCompletionScore());
    }

    /**
     * Property 19: 匿名评价隐私保护
     * For any 标记为匿名的评价，查询评价时不应该返回评价者的身份信息
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 19: 匿名评价隐私保护")
    void anonymousEvaluationShouldHideEvaluatorIdentity(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long evaluatorId,
            @ForAll("differentUserIds") Long evaluatedId) {
        
        // Skip if evaluator and evaluated are the same
        if (evaluatorId.equals(evaluatedId)) {
            return;
        }
        
        // Setup
        setUp();
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(1L);
        when(teamMapper.selectById(teamId)).thenReturn(team);
        
        // Create anonymous evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setId(1L);
        evaluation.setProjectId(1L);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setEvaluatedId(evaluatedId);
        evaluation.setTechContributionScore(4);
        evaluation.setCollaborationScore(4);
        evaluation.setTaskCompletionScore(4);
        evaluation.setIsAnonymous(true);
        evaluation.setCreatedAt(LocalDateTime.now());
        
        List<Evaluation> evaluations = new ArrayList<>();
        evaluations.add(evaluation);
        
        when(evaluationMapper.selectList(any(Wrapper.class))).thenReturn(evaluations);
        
        User evaluatedUser = new User();
        evaluatedUser.setId(evaluatedId);
        evaluatedUser.setUsername("evaluated_user");
        when(userMapper.selectById(evaluatedId)).thenReturn(evaluatedUser);
        
        // Execute
        List<EvaluationVO> result = evaluationService.getEvaluations(teamId);
        
        // Verify: Evaluator name should be null for anonymous evaluation
        assertFalse(result.isEmpty(), "Should return evaluation");
        EvaluationVO vo = result.get(0);
        assertTrue(vo.getIsAnonymous(), "Should be marked as anonymous");
        assertNull(vo.getEvaluatorName(), "Evaluator name should be null for anonymous evaluation");
    }

    /**
     * Property 20: 评价评论可选性
     * For any 评价提交，文字评论字段应该是可选的，可以为空或包含内容
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 20: 评价评论可选性")
    void evaluationCommentShouldBeOptional(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long evaluatorId,
            @ForAll("differentUserIds") Long evaluatedId,
            @ForAll("optionalComments") String comment) {
        
        // Skip if evaluator and evaluated are the same
        if (evaluatorId.equals(evaluatedId)) {
            return;
        }
        
        // Setup
        setUp();
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(1L);
        when(teamMapper.selectById(teamId)).thenReturn(team);
        
        User evaluatedUser = new User();
        evaluatedUser.setId(evaluatedId);
        evaluatedUser.setUsername("evaluated_user");
        when(userMapper.selectById(evaluatedId)).thenReturn(evaluatedUser);
        
        when(evaluationMapper.countByProjectAndEvaluatorAndEvaluated(anyLong(), anyLong(), anyLong())).thenReturn(0);
        
        // Track captured evaluation
        final Evaluation[] capturedEvaluation = {null};
        when(evaluationMapper.insert(any(Evaluation.class))).thenAnswer(invocation -> {
            capturedEvaluation[0] = invocation.getArgument(0);
            return 1;
        });
        
        // Create DTO with optional comment
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(evaluatedId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(4);
        dto.setTaskCompletionScore(4);
        dto.setComment(comment);
        
        // Execute: Should succeed regardless of comment being null or not
        assertDoesNotThrow(
            () -> evaluationService.submitEvaluation(teamId, evaluatorId, dto),
            "Evaluation should succeed with optional comment"
        );
        
        // Verify: Comment should be saved as provided (null or with content)
        assertNotNull(capturedEvaluation[0], "Evaluation should be created");
        assertEquals(comment, capturedEvaluation[0].getComment(), "Comment should match input");
    }

    /**
     * Property 21: 防止重复评价
     * For any 用户和被评价者组合，在同一个项目中只能提交一次评价，重复提交应该被拒绝
     * Validates: Requirements 6.5
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 21: 防止重复评价")
    void shouldPreventDuplicateEvaluation(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long evaluatorId,
            @ForAll("differentUserIds") Long evaluatedId) {
        
        // Skip if evaluator and evaluated are the same
        if (evaluatorId.equals(evaluatedId)) {
            return;
        }
        
        // Setup: Simulate existing evaluation
        setUp();
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(1L);
        when(teamMapper.selectById(teamId)).thenReturn(team);
        
        User evaluatedUser = new User();
        evaluatedUser.setId(evaluatedId);
        evaluatedUser.setUsername("evaluated_user");
        when(userMapper.selectById(evaluatedId)).thenReturn(evaluatedUser);
        
        // Return 1 to indicate existing evaluation
        when(evaluationMapper.countByProjectAndEvaluatorAndEvaluated(1L, evaluatorId, evaluatedId)).thenReturn(1);
        
        // Create DTO
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(evaluatedId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(4);
        dto.setTaskCompletionScore(4);
        
        // Execute & Verify: Should throw BusinessException for duplicate evaluation
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> evaluationService.submitEvaluation(teamId, evaluatorId, dto),
            "Should reject duplicate evaluation"
        );
        
        assertTrue(
            exception.getMessage().contains("已经评价过"),
            "Error message should indicate duplicate evaluation"
        );
    }

    /**
     * Property 22: 防止自我评价
     * For any 评价提交，评价者 ID 和被评价者 ID 不能相同，否则应该被拒绝
     * Validates: Requirements 6.6
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 22: 防止自我评价")
    void shouldPreventSelfEvaluation(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long userId) {
        
        // Setup
        setUp();
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(1L);
        when(teamMapper.selectById(teamId)).thenReturn(team);
        
        // Create DTO with same evaluator and evaluated
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(userId);
        dto.setTechContributionScore(4);
        dto.setCollaborationScore(4);
        dto.setTaskCompletionScore(4);
        
        // Execute & Verify: Should throw BusinessException for self-evaluation
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> evaluationService.submitEvaluation(teamId, userId, dto),
            "Should reject self-evaluation"
        );
        
        assertTrue(
            exception.getMessage().contains("不能评价自己"),
            "Error message should indicate self-evaluation is not allowed"
        );
    }

    /**
     * Property 23: 评价提交成功响应
     * For any 有效的评价提交，系统应该返回成功响应（HTTP 200）
     * Validates: Requirements 6.7
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 23: 评价提交成功响应")
    void validEvaluationSubmissionShouldSucceed(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long evaluatorId,
            @ForAll("differentUserIds") Long evaluatedId,
            @ForAll("validScores") Integer techScore,
            @ForAll("validScores") Integer collabScore,
            @ForAll("validScores") Integer taskScore) {
        
        // Skip if evaluator and evaluated are the same
        if (evaluatorId.equals(evaluatedId)) {
            return;
        }
        
        // Setup
        setUp();
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(1L);
        when(teamMapper.selectById(teamId)).thenReturn(team);
        
        User evaluatedUser = new User();
        evaluatedUser.setId(evaluatedId);
        evaluatedUser.setUsername("evaluated_user");
        when(userMapper.selectById(evaluatedId)).thenReturn(evaluatedUser);
        
        when(evaluationMapper.countByProjectAndEvaluatorAndEvaluated(anyLong(), anyLong(), anyLong())).thenReturn(0);
        when(evaluationMapper.insert(any(Evaluation.class))).thenReturn(1);
        
        // Create valid DTO
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatedId(evaluatedId);
        dto.setTechContributionScore(techScore);
        dto.setCollaborationScore(collabScore);
        dto.setTaskCompletionScore(taskScore);
        
        // Execute & Verify: Should complete without throwing exception
        assertDoesNotThrow(
            () -> evaluationService.submitEvaluation(teamId, evaluatorId, dto),
            "Valid evaluation submission should succeed"
        );
        
        // Verify that insert was called
        verify(evaluationMapper, times(1)).insert(any(Evaluation.class));
    }

    // ===== Arbitraries (Data Generators) =====

    @Provide
    Arbitrary<Long> teamIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 100L);
    }

    @Provide
    Arbitrary<Long> differentUserIds() {
        return Arbitraries.longs().between(101L, 200L);
    }

    @Provide
    Arbitrary<Integer> validScores() {
        return Arbitraries.integers().between(1, 5);
    }

    @Provide
    Arbitrary<Integer> invalidScores() {
        return Arbitraries.oneOf(
            Arbitraries.integers().between(Integer.MIN_VALUE, 0),
            Arbitraries.integers().between(6, Integer.MAX_VALUE)
        );
    }

    @Provide
    Arbitrary<String> optionalComments() {
        return Arbitraries.oneOf(
            Arbitraries.just((String) null),
            Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(200)
        );
    }
   
}
