package com.teamup.server.modules.team.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.service.TeamSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 团队设置控制器
 */
@RestController
@RequestMapping("/teams/{teamId}/settings")
@RequiredArgsConstructor
public class TeamSettingController {
    
    private final TeamSettingService settingService;
    
    /**
     * 获取团队所有设置
     */
    @GetMapping
    public Result<Map<String, String>> getAllSettings(@PathVariable Long teamId) {
        return Result.success(settingService.getAllSettings(teamId));
    }
    
    /**
     * 获取单个设置
     */
    @GetMapping("/{key}")
    public Result<String> getSetting(@PathVariable Long teamId, @PathVariable String key) {
        return Result.success(settingService.getSetting(teamId, key));
    }
    
    /**
     * 保存单个设置
     */
    @PutMapping("/{key}")
    public Result<Void> saveSetting(
            @PathVariable Long teamId,
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        settingService.saveSetting(teamId, key, body.get("value"));
        return Result.success();
    }
    
    /**
     * 批量保存设置
     */
    @PutMapping
    public Result<Void> batchSaveSettings(
            @PathVariable Long teamId,
            @RequestBody Map<String, String> settings) {
        settingService.batchSaveSettings(teamId, settings);
        return Result.success();
    }
    
    /**
     * 删除设置
     */
    @DeleteMapping("/{key}")
    public Result<Void> deleteSetting(@PathVariable Long teamId, @PathVariable String key) {
        settingService.deleteSetting(teamId, key);
        return Result.success();
    }
}
