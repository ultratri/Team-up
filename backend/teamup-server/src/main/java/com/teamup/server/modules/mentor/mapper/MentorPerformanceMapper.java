package com.teamup.server.modules.mentor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.mentor.entity.MentorPerformance;
import com.teamup.server.modules.mentor.vo.MentorDetailVO;
import com.teamup.server.modules.mentor.vo.MentorInfoVO;
import com.teamup.server.modules.mentor.vo.MentorRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 导师绩效Mapper
 */
@Mapper
public interface MentorPerformanceMapper extends BaseMapper<MentorPerformance> {
    
    /**
     * 查询导师列表（带用户信息）
     */
    @Select("SELECT " +
            "u.id, u.username, " +
            "up.real_name, up.department, up.major, " +
            "COALESCE(mp.total_mentees, 0) as total_mentees, " +
            "COALESCE(mp.active_mentees, 0) as active_mentees, " +
            "COALESCE(mp.completed_mentees, 0) as completed_mentees, " +
            "COALESCE(mp.successful_mentees, 0) as successful_mentees, " +
            "COALESCE(mp.average_mentee_score, 0) as average_mentee_score, " +
            "COALESCE(mp.total_reward_points, 0) as total_reward_points, " +
            "COALESCE(mp.rating, 0) as rating " +
            "FROM users u " +
            "INNER JOIN user_roles ur ON u.id = ur.user_id AND ur.role_name = 'MENTOR' " +
            "LEFT JOIN user_profiles up ON u.id = up.user_id " +
            "LEFT JOIN mentor_performance mp ON u.id = mp.mentor_id " +
            "ORDER BY u.id DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<MentorInfoVO> selectMentorList(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 查询导师总数
     */
    @Select("SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur WHERE ur.role_name = 'MENTOR'")
    long countMentors();
    
    /**
     * 查询导师排行榜
     */
    @Select("SELECT " +
            "u.id as mentor_id, " +
            "up.real_name as mentor_name, " +
            "up.department, " +
            "COALESCE(mp.successful_mentees, 0) as successful_mentees, " +
            "COALESCE(mp.average_mentee_score, 0) as average_mentee_score, " +
            "COALESCE(mp.total_reward_points, 0) as total_reward_points, " +
            "COALESCE(mp.rating, 0) as rating " +
            "FROM users u " +
            "INNER JOIN user_roles ur ON u.id = ur.user_id AND ur.role_name = 'MENTOR' " +
            "LEFT JOIN user_profiles up ON u.id = up.user_id " +
            "LEFT JOIN mentor_performance mp ON u.id = mp.mentor_id " +
            "ORDER BY mp.rating DESC, mp.successful_mentees DESC " +
            "LIMIT #{limit}")
    List<MentorRankingVO> selectMentorRanking(@Param("limit") int limit);
    
    /**
     * 查询导师详情
     */
    @Select("SELECT " +
            "u.id, u.username, u.email, u.phone, " +
            "up.real_name, up.department, up.major, up.bio, " +
            "COALESCE(mp.total_mentees, 0) as total_mentees, " +
            "COALESCE(mp.active_mentees, 0) as active_mentees, " +
            "COALESCE(mp.completed_mentees, 0) as completed_mentees, " +
            "COALESCE(mp.successful_mentees, 0) as successful_mentees, " +
            "COALESCE(mp.average_mentee_score, 0) as average_mentee_score, " +
            "COALESCE(mp.total_reward_points, 0) as total_reward_points, " +
            "COALESCE(mp.rating, 0) as rating, " +
            "ur.granted_at as became_mentor_at, " +
            "ma.application_reason as application_reason " +
            "FROM users u " +
            "INNER JOIN user_roles ur ON u.id = ur.user_id AND ur.role_name = 'MENTOR' " +
            "LEFT JOIN user_profiles up ON u.id = up.user_id " +
            "LEFT JOIN mentor_performance mp ON u.id = mp.mentor_id " +
            "LEFT JOIN mentor_applications ma ON u.id = ma.applicant_id AND ma.status = 'APPROVED' " +
            "WHERE u.id = #{mentorId}")
    MentorDetailVO selectMentorDetail(@Param("mentorId") Long mentorId);
}
