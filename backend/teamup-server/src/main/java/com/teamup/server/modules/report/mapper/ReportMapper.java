package com.teamup.server.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.report.entity.Report;
import com.teamup.server.modules.report.vo.ReportDetailVO;
import com.teamup.server.modules.report.vo.ReportStatisticsVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 举报Mapper
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
    
    /**
     * 查询举报详情（包含举报人和目标信息）
     */
    @Select("SELECT " +
            "r.*, " +
            "u1.username as reporter_name, " +
            "u2.username as handler_name " +
            "FROM reports r " +
            "LEFT JOIN users u1 ON r.reporter_id = u1.id " +
            "LEFT JOIN users u2 ON r.handler_id = u2.id " +
            "WHERE r.id = #{reportId}")
    ReportDetailVO selectReportDetail(@Param("reportId") Long reportId);
    
    /**
     * 查询举报统计信息
     */
    @Select("SELECT " +
            "target_type, " +
            "status, " +
            "COUNT(*) as count " +
            "FROM reports " +
            "GROUP BY target_type, status")
    List<ReportStatisticsVO> selectReportStatistics();
    
    /**
     * 删除项目（软删除或硬删除根据业务需求）
     * 注意: 这里使用UPDATE设置状态为已删除,而不是真正删除数据
     */
    @Update("UPDATE projects SET status = 'DELETED', updated_at = NOW() WHERE id = #{projectId}")
    int deleteProject(@Param("projectId") Long projectId);
    
    /**
     * 删除团队（软删除）
     */
    @Update("UPDATE teams SET status = 'DELETED', updated_at = NOW() WHERE id = #{teamId}")
    int deleteTeam(@Param("teamId") Long teamId);
    
    /**
     * 删除评论（硬删除）
     */
    @Delete("DELETE FROM project_comments WHERE id = #{commentId}")
    int deleteComment(@Param("commentId") Long commentId);
    
    /**
     * 查询项目创建者ID
     */
    @Select("SELECT creator_id FROM projects WHERE id = #{projectId}")
    Long getProjectCreatorId(@Param("projectId") Long projectId);
    
    /**
     * 查询团队创建者ID
     */
    @Select("SELECT creator_id FROM teams WHERE id = #{teamId}")
    Long getTeamCreatorId(@Param("teamId") Long teamId);
    
    /**
     * 查询评论作者ID
     */
    @Select("SELECT user_id FROM project_comments WHERE id = #{commentId}")
    Long getCommentAuthorId(@Param("commentId") Long commentId);
}
