package com.teamup.server.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.report.entity.Report;
import com.teamup.server.modules.report.vo.ReportDetailVO;
import com.teamup.server.modules.report.vo.ReportStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
