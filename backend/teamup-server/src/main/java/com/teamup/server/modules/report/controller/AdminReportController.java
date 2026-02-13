package com.teamup.server.modules.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.report.dto.HandleReportDTO;
import com.teamup.server.modules.report.entity.Report;
import com.teamup.server.modules.report.service.ReportService;
import com.teamup.server.modules.report.vo.ReportDetailVO;
import com.teamup.server.modules.report.vo.ReportStatisticsVO;
import com.teamup.server.modules.user.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-举报管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminReportController {
    
    private final ReportService reportService;
    
    /**
     * 查询举报列表
     */
    @GetMapping
    public Result<Page<ReportDetailVO>> listReports(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Report.ReportStatus status,
        @RequestParam(required = false) Report.TargetType targetType
    ) {
        Page<ReportDetailVO> result = reportService.listReports(page, size, status, targetType);
        return Result.success(result);
    }
    
    /**
     * 查询举报详情
     */
    @GetMapping("/{reportId}")
    public Result<ReportDetailVO> getReportDetail(@PathVariable Long reportId) {
        ReportDetailVO detail = reportService.getReportDetail(reportId);
        return Result.success(detail);
    }
    
    /**
     * 处理举报
     */
    @PostMapping("/handle")
    public Result<Void> handleReport(@Valid @RequestBody HandleReportDTO dto) {
        Long adminId = UserContext.getCurrentUserId();
        reportService.handleReport(adminId, dto);
        return Result.success();
    }
    
    /**
     * 查询举报统计
     */
    @GetMapping("/statistics")
    public Result<List<ReportStatisticsVO>> getStatistics() {
        List<ReportStatisticsVO> statistics = reportService.getReportStatistics();
        return Result.success(statistics);
    }
}
