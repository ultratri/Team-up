package com.teamup.server.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.evaluation.dto.EvaluationDTO;
import com.teamup.server.modules.evaluation.entity.Evaluation;
import com.teamup.server.modules.evaluation.mapper.EvaluationMapper;
import com.teamup.server.modules.evaluation.service.EvaluationService;
import com.teamup.server.modules.evaluation.vo.EvaluationVO;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 评价服务实现
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {
    
    @Resource
    private EvaluationMapper evaluationMapper;
    
    @Resource
    private TeamMapper teamMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitEvaluation(Long teamId, Long evaluatorId, EvaluationDTO dto) {
        // 1. 验证团队是否存在
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("团队不存在");
        }
        
        Long projectId = team.getProjectId();
        if (projectId == null) {
            throw new BusinessException("团队未关联项目");
        }
        
        // 2. 验证评价分数范围（1-5）
        validateScore(dto.getTechContributionScore(), "技术贡献分数");
        validateScore(dto.getCollaborationScore(), "协作能力分数");
        validateScore(dto.getTaskCompletionScore(), "任务完成分数");
        
        // 3. 防止自我评价
        if (evaluatorId.equals(dto.getEvaluatedId())) {
            throw new BusinessException("不能评价自己");
        }
        
        // 4. 防止重复评价
        int count = evaluationMapper.countByProjectAndEvaluatorAndEvaluated(
            projectId, evaluatorId, dto.getEvaluatedId()
        );
        if (count > 0) {
            throw new BusinessException("已经评价过该成员");
        }
        
        // 5. 验证被评价者是否存在
        User evaluatedUser = userMapper.selectById(dto.getEvaluatedId());
        if (evaluatedUser == null) {
            throw new BusinessException("被评价者不存在");
        }
        
        // 6. 创建评价记录
        Evaluation evaluation = new Evaluation();
        evaluation.setProjectId(projectId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setEvaluatedId(dto.getEvaluatedId());
        evaluation.setTechContributionScore(dto.getTechContributionScore());
        evaluation.setCollaborationScore(dto.getCollaborationScore());
        evaluation.setTaskCompletionScore(dto.getTaskCompletionScore());
        evaluation.setComment(dto.getComment());
        
        // 7. 处理匿名评价逻辑
        if (dto.getIsAnonymous() != null && dto.getIsAnonymous()) {
            evaluation.setIsAnonymous(true);
        } else {
            evaluation.setIsAnonymous(false);
        }
        
        evaluationMapper.insert(evaluation);
    }
    
    @Override
    public List<EvaluationVO> getEvaluations(Long teamId) {
        // 1. 验证团队是否存在
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("团队不存在");
        }
        
        Long projectId = team.getProjectId();
        if (projectId == null) {
            return new ArrayList<>();
        }
        
        // 2. 查询评价列表
        QueryWrapper<Evaluation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId);
        queryWrapper.orderByDesc("created_at");
        List<Evaluation> evaluations = evaluationMapper.selectList(queryWrapper);
        
        // 3. 转换为VO
        List<EvaluationVO> voList = new ArrayList<>();
        for (Evaluation evaluation : evaluations) {
            EvaluationVO vo = new EvaluationVO();
            BeanUtils.copyProperties(evaluation, vo);
            
            // 获取被评价者信息
            User evaluatedUser = userMapper.selectById(evaluation.getEvaluatedId());
            if (evaluatedUser != null) {
                vo.setEvaluatedName(evaluatedUser.getUsername());
            }
            
            // 处理匿名评价
            if (evaluation.getIsAnonymous() != null && evaluation.getIsAnonymous()) {
                vo.setEvaluatorName(null);
            } else {
                User evaluatorUser = userMapper.selectById(evaluation.getEvaluatorId());
                if (evaluatorUser != null) {
                    vo.setEvaluatorName(evaluatorUser.getUsername());
                }
            }
            
            voList.add(vo);
        }
        
        return voList;
    }
    
    @Override
    public boolean canEvaluate(Long evaluatorId, Long evaluatedId, Long projectId) {
        // 1. 不能评价自己
        if (evaluatorId.equals(evaluatedId)) {
            return false;
        }
        
        // 2. 检查是否已经评价过
        int count = evaluationMapper.countByProjectAndEvaluatorAndEvaluated(
            projectId, evaluatorId, evaluatedId
        );
        
        return count == 0;
    }
    
    /**
     * 验证评价分数范围
     */
    private void validateScore(Integer score, String fieldName) {
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException(fieldName + "必须在1-5之间");
        }
    }
}
