package com.teamup.server.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.project.entity.ProjectRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectRecommendationMapper extends BaseMapper<ProjectRecommendation> {
    
    /**
     * 查询项目的推荐列表（包含用户简要信息）
     */
    @Select("SELECT r.*, u.username, u.user_code, p.avatar_url, p.department, p.major " +
            "FROM project_recommendations r " +
            "LEFT JOIN users u ON r.user_id = u.id " +
            "LEFT JOIN user_profiles p ON r.user_id = p.user_id " +
            "WHERE r.project_id = #{projectId} " +
            "ORDER BY r.match_score DESC")
    List<Map<String, Object>> selectRecommendationsWithUserInfo(Long projectId);
}
