package com.teamup.server.modules.report.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.report.dto.SubmitReportDTO;
import com.teamup.server.modules.report.service.ReportService;
import com.teamup.server.modules.user.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 举报控制器（用户端）
 */
@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReportController {
    
    private final ReportService reportService;
    
    /**
     * 提交举报
     */
    @PostMapping
    public Result<Void> submitReport(@Valid @RequestBody SubmitReportDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        reportService.submitReport(userId, dto);
        return Result.success();
    }
}
