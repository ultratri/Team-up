package com.teamup.server.modules.mentor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.mentor.dto.MentorMemberEvaluationDTO;
import com.teamup.server.modules.mentor.entity.MentorMemberEvaluation;
import com.teamup.server.modules.mentor.mapper.MentorMemberEvaluationMapper;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导师成员评价服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentorMemberEvaluationService {
    
    private final MentorMemberEvaluationMapper evaluationMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;
    
    /**
     * 获取团队成员列表（供导师评价）
     */
    public List<Map<String, Object>> getTeamMembersForEvaluation(Long teamId, Long mentorId) {
        // 验证团队存在且导师是该团队的导师
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("团队不存在");
        }
        if (!mentorId.equals(team.getMentorId())) {
            throw new BusinessException("您不是该团队的导师");
        }
        
        // 获取团队成员
        List<TeamMember> members = teamMemberMapper.selectList(
            new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
        );
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeamMember member : members) {
            User user = userMapper.selectById(member.getUserId());
            if (user == null) continue;
            
            // 查询是否已有评价
            MentorMemberEvaluation evaluation = evaluationMapper.selectOne(
                new LambdaQueryWrapper<MentorMemberEvaluation>()
                    .eq(MentorMemberEvaluation::getTeamId, teamId)
                    .eq(MentorMemberEvaluation::getMentorId, mentorId)
                    .eq(MentorMemberEvaluation::getMemberId, member.getUserId())
            );
            
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("userId", user.getId());
            memberInfo.put("userName", user.getUsername());
            memberInfo.put("realName", user.getUsername()); // User实体没有realName字段，使用username
            memberInfo.put("role", member.getRole());
            memberInfo.put("creditScore", getUserCreditScore(user.getId())); // 从UserCredit表获取
            
            // 如果已有评价，返回评价信息
            if (evaluation != null) {
                memberInfo.put("evaluated", true);
                memberInfo.put("score", evaluation.getScore());
                memberInfo.put("technicalAbility", evaluation.getTechnicalAbility());
                memberInfo.put("collaboration", evaluation.getCollaboration());
                memberInfo.put("learningAttitude", evaluation.getLearningAttitude());
                memberInfo.put("taskCompletion", evaluation.getTaskCompletion());
                memberInfo.put("comment", evaluation.getComment());
                memberInfo.put("evaluatedAt", evaluation.getUpdatedAt());
            } else {
                memberInfo.put("evaluated", false);
            }
            
            result.add(memberInfo);
        }
        
        return result;
    }
    
    /**
     * 提交或更新对单个成员的评价
     */
    @Transactional(rollbackFor = Exception.class)
    public void evaluateMember(Long teamId, Long mentorId, MentorMemberEvaluationDTO dto) {
        // 验证团队和导师
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("团队不存在");
        }
        if (!mentorId.equals(team.getMentorId())) {
            throw new BusinessException("您不是该团队的导师");
        }
        
        // 验证成员在团队中
        TeamMember member = teamMemberMapper.selectOne(
            new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, dto.getMemberId())
        );
        if (member == null) {
            throw new BusinessException("该用户不是团队成员");
        }
        
        // 查询是否已有评价
        MentorMemberEvaluation existing = evaluationMapper.selectOne(
            new LambdaQueryWrapper<MentorMemberEvaluation>()
                .eq(MentorMemberEvaluation::getTeamId, teamId)
                .eq(MentorMemberEvaluation::getMentorId, mentorId)
                .eq(MentorMemberEvaluation::getMemberId, dto.getMemberId())
        );
        
        if (existing != null) {
            // 更新评价
            existing.setScore(dto.getScore());
            existing.setTechnicalAbility(dto.getTechnicalAbility());
            existing.setCollaboration(dto.getCollaboration());
            existing.setLearningAttitude(dto.getLearningAttitude());
            existing.setTaskCompletion(dto.getTaskCompletion());
            existing.setComment(dto.getComment());
            evaluationMapper.updateById(existing);
            
            log.info("导师更新成员评价: mentorId={}, teamId={}, memberId={}, score={}", 
                    mentorId, teamId, dto.getMemberId(), dto.getScore());
        } else {
            // 新增评价
            MentorMemberEvaluation evaluation = new MentorMemberEvaluation();
            evaluation.setTeamId(teamId);
            evaluation.setMentorId(mentorId);
            evaluation.setMemberId(dto.getMemberId());
            evaluation.setScore(dto.getScore());
            evaluation.setTechnicalAbility(dto.getTechnicalAbility());
            evaluation.setCollaboration(dto.getCollaboration());
            evaluation.setLearningAttitude(dto.getLearningAttitude());
            evaluation.setTaskCompletion(dto.getTaskCompletion());
            evaluation.setComment(dto.getComment());
            evaluationMapper.insert(evaluation);
            
            log.info("导师新增成员评价: mentorId={}, teamId={}, memberId={}, score={}", 
                    mentorId, teamId, dto.getMemberId(), dto.getScore());
        }
        
        // 影响成员信誉分
        updateMemberCredit(dto.getMemberId(), dto.getScore());
    }
    
    /**
     * 批量提交评价
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchEvaluateMembers(Long teamId, Long mentorId, List<MentorMemberEvaluationDTO> evaluations) {
        for (MentorMemberEvaluationDTO dto : evaluations) {
            evaluateMember(teamId, mentorId, dto);
        }
    }
    
    /**
     * 根据导师评分更新成员信誉分
     * 评分规则：
     * - 90-100分：+3分
     * - 80-89分：+2分
     * - 70-79分：+1分
     * - 60-69分：0分
     * - 60分以下：-2分
     */
    private void updateMemberCredit(Long memberId, Integer score) {
        int creditChange = 0;
        String reason = "";
        
        if (score >= 90) {
            creditChange = 3;
            reason = "导师评价优秀";
        } else if (score >= 80) {
            creditChange = 2;
            reason = "导师评价良好";
        } else if (score >= 70) {
            creditChange = 1;
            reason = "导师评价合格";
        } else if (score >= 60) {
            creditChange = 0;
            reason = "导师评价及格";
        } else {
            creditChange = -2;
            reason = "导师评价不及格";
        }
        
        if (creditChange != 0) {
            creditService.addCreditRecord(memberId, creditChange, "MENTOR_EVALUATION", null, reason);
            log.info("导师评价影响信誉分: memberId={}, score={}, creditChange={}", 
                    memberId, score, creditChange);
        }
    }
    
    /**
     * 获取用户信誉分
     */
    private Integer getUserCreditScore(Long userId) {
        // 从UserCredit表查询
        com.teamup.server.modules.user.entity.UserCredit credit = 
            creditService.getUserCredit(userId);
        return credit != null ? credit.getTotalCredit() : 60; // 默认60分
    }
}
