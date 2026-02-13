package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.user.entity.UserAvailability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户可用性Mapper
 */
@Mapper
public interface UserAvailabilityMapper extends BaseMapper<UserAvailability> {
    
    /**
     * 根据用户ID查询组队意向
     * @param userId 用户ID
     * @return 用户可用性信息
     */
    @Select("SELECT * FROM user_availability WHERE user_id = #{userId}")
    UserAvailability selectByUserId(Long userId);
    
    /**
     * 优化的人才列表查询 - 使用 JOIN 一次性获取所有数据
     * 这个方法通过 JOIN 减少数据库往返次数，提高查询性能
     * 
     * @param page 分页对象
     * @param visibilityConditions 可见范围条件（例如：'PUBLIC' 或 'PUBLIC','MENTOR'）
     * @param intention 组队意向筛选（可选）
     * @return 用户ID列表（已按条件筛选和排序）
     */
    @Select("<script>" +
            "SELECT DISTINCT ua.user_id " +
            "FROM user_availability ua " +
            "INNER JOIN users u ON ua.user_id = u.id " +
            "INNER JOIN user_profiles up ON ua.user_id = up.user_id " +
            "LEFT JOIN user_credits uc ON ua.user_id = uc.user_id " +
            "WHERE ua.is_available = 1 " +
            "AND u.status = 'ACTIVE' " +
            "AND up.real_name IS NOT NULL AND up.real_name != '' " +
            "AND up.department IS NOT NULL AND up.department != '' " +
            "AND up.major IS NOT NULL AND up.major != '' " +
            "AND ua.visibility IN " +
            "<foreach collection='visibilityConditions' item='vis' open='(' separator=',' close=')'>" +
            "#{vis}" +
            "</foreach> " +
            "<if test='intention != null and intention != \"\"'>" +
            "AND ua.intention LIKE CONCAT('%', #{intention}, '%') " +
            "</if>" +
            "ORDER BY " +
            "COALESCE(uc.total_credit, 0) DESC, " +
            "u.last_login_at DESC " +
            "</script>")
    Page<Long> selectTalentUserIds(Page<Long> page, 
                                   @Param("visibilityConditions") List<String> visibilityConditions,
                                   @Param("intention") String intention);
}
