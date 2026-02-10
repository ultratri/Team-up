package com.teamup.server.modules.stats.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.stats.dto.StatsOverviewDTO;
import com.teamup.server.modules.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计控制器
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 获取统计总览
     */
    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public Result<StatsOverviewDTO> getOverview() {
        StatsOverviewDTO overview = statsService.getOverview();
        return Result.success(overview);
    }
}
