package com.teamup.server.modules.mentor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.mentor.entity.MentorReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 学员评价导师Mapper
 */
@Mapper
public interface MentorReviewMapper extends BaseMapper<MentorReview> {
    
    /**
     * 计算导师的平均评分
     * 
     * @param mentorId 导师ID
     * @return 平均评分
     */
    @Select("SELECT AVG(overall_rating) FROM mentor_reviews " +
            "WHERE mentor_id = #{mentorId} AND status = 'ACTIVE'")
    BigDecimal getAverageRating(@Param("mentorId") Long mentorId);
    
    /**
     * 获取导师的评价数量
     * 
     * @param mentorId 导师ID
     * @return 评价数量
     */
    @Select("SELECT COUNT(*) FROM mentor_reviews " +
            "WHERE mentor_id = #{mentorId} AND status = 'ACTIVE'")
    Integer getReviewCount(@Param("mentorId") Long mentorId);
}
