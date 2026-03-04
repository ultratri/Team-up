package com.teamup.server.modules.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.evaluation.entity.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 评价Mapper
 */
@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {
    
    /**
     * 获取用户的平均评分
     */
    @Select("SELECT " +
            "COALESCE(AVG(tech_contribution_score), 0) as avgTech, " +
            "COALESCE(AVG(collaboration_score), 0) as avgCollab, " +
            "COALESCE(AVG(task_completion_score), 0) as avgTask " +
            "FROM evaluations WHERE evaluated_id = #{userId}")
    Map<String, Double> getAverageScores(@Param("userId") Long userId);
    
    /**
     * 统计特定项目中评价者对被评价者的评价次数
     */
    @Select("SELECT COUNT(*) FROM evaluations " +
            "WHERE project_id = #{projectId} " +
            "AND evaluator_id = #{evaluatorId} " +
            "AND evaluated_id = #{evaluatedId}")
    int countByProjectAndEvaluatorAndEvaluated(
        @Param("projectId") Long projectId,
        @Param("evaluatorId") Long evaluatorId,
        @Param("evaluatedId") Long evaluatedId
    );
}
