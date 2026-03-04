package com.teamup.server.modules.mentor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.mentor.entity.MentorMemberEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 导师成员评价Mapper
 */
@Mapper
public interface MentorMemberEvaluationMapper extends BaseMapper<MentorMemberEvaluation> {

    /**
     * 获取成员的导师评价聚合统计（均值）
     * - avgScore: 0-100
     * - avgTech/avgCollab/avgLearning/avgTask: 1-5（可能为空）
     * - evalCount: 评价条数
     */
    @Select("SELECT " +
            "COUNT(*) as evalCount, " +
            "COALESCE(AVG(score), 0) as avgScore, " +
            "COALESCE(AVG(technical_ability), 0) as avgTech, " +
            "COALESCE(AVG(collaboration), 0) as avgCollab, " +
            "COALESCE(AVG(learning_attitude), 0) as avgLearning, " +
            "COALESCE(AVG(task_completion), 0) as avgTask " +
            "FROM mentor_member_evaluations WHERE member_id = #{memberId}")
    Map<String, Object> getAverageStatsByMemberId(@Param("memberId") Long memberId);
}
