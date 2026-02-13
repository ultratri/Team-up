package com.teamup.server.modules.system.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/system/settings")
@RequiredArgsConstructor
public class SystemSettingController {
    
    private final SystemSettingService service;
    
    /**
     * 获取所有系统设置（按分组）
     */
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<Map<String, Map<String, Object>>> getAllSettings() {
        return Result.success(service.getAllSettings());
    }
    
    /**
     * 获取指定分组的设置（公开访问，用于前端显示站点信息）
     */
    @GetMapping("/{group}")
    public Result<Map<String, Object>> getSettingsByGroup(@PathVariable String group) {
        return Result.success(service.getSettingsByGroup(group));
    }
    
    /**
     * 保存指定分组的设置
     */
    @PutMapping("/{group}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<Void> saveSettings(
            @PathVariable String group,
            @RequestBody Map<String, Object> settings) {
        service.saveSettings(group, settings);
        return Result.success();
    }
}
