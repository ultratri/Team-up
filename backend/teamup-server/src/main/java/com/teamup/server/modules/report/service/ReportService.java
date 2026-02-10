package com.teamup.server.modules.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.report.dto.HandleReportDTO;
import com.teamup.server.modules.report.dto.SubmitReportDTO;
import com.teamup.server.modules.report.entity.Report;
import com.teamup.server.modules.report.vo.ReportDetailVO;
import com.teamup.server.modules.report.vo.ReportStatisticsVO;

import java.util.List;

/**
 * 举报服务接口
 */
public interface ReportService {
    
    /**
     * 提交举报
     */
    void submitReport(Long reporterId, SubmitReportDTO dto);
    
    /**
     * 查询举报列表（管理员）
     */
    Page<ReportDetailVO> listReports(int page, int size, Report.ReportStatus status, Report.TargetType targetType);
    
    /**
     * 查询举报详情
     */
    ReportDetailVO getReportDetail(Long reportId);
    
    /**
     * 处理举报（管理员）
     */
    void handleReport(Long handlerId, HandleReportDTO dto);
    
    /**
     * 查询举报统计信息
     */
    List<ReportStatisticsVO> getReportStatistics();
}
