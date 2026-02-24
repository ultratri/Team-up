package com.teamup.server.modules.team.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.entity.DailyStandup;
import com.teamup.server.modules.team.service.DailyStandupService;
import com.teamup.server.modules.team.vo.DailyStandupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日站会控制器
 */
@RestController
@RequestMapping("/standups")
@RequiredArgsConstructor
public class DailyStandupController {
    
    private final DailyStandupService standupService;
    
    /**
     * 提交站会记录
     */
    @PostMapping
    public Result<DailyStandup> submitStandup(@RequestBody DailyStandup standup) {
        return Result.success(standupService.submitStandup(standup));
    }
    
    /**
     * 更新站会记录
     */
    @PutMapping("/{id}")
    public Result<DailyStandup> updateStandup(@PathVariable Long id, @RequestBody DailyStandup standup) {
        standup.setId(id);
        return Result.success(standupService.updateStandup(standup));
    }
    
    /**
     * 获取团队某日的站会记录
     */
    @GetMapping("/team/{teamId}")
    public Result<List<DailyStandupVO>> getTeamStandups(
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(standupService.getTeamStandups(teamId, date));
    }
    
    /**
     * 获取用户的站会记录
     */
    @GetMapping("/user/{userId}")
    public Result<List<DailyStandupVO>> getUserStandups(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(standupService.getUserStandups(userId, startDate, endDate));
    }
}
