package com.teamup.server.modules.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.evaluation.entity.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评价Mapper
 */
@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {
    
    /**
     * 检查是否已经评价过
     */
    @Select("SELECT COUNT(*) FROM evaluations WHERE project_id = #{projectId} AND evaluator_id = #{evaluatorId} AND evaluated_id = #{evaluatedId}")
    int countByProjectAndEvaluatorAndEvaluated(@Param("projectId") Long projectId, 
                                                @Param("evaluatorId") Long evaluatorId, 
                                                @Param("evaluatedId") Long evaluatedId);
}
