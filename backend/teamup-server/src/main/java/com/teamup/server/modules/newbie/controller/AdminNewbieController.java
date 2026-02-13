package com.teamup.server.modules.newbie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.newbie.entity.NewbieConfig;
import com.teamup.server.modules.newbie.entity.NewbieTask;
import com.teamup.server.modules.newbie.service.NewbieProtectionService;
import com.teamup.server.modules.newbie.vo.SkillCertificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 - 新手保护管理控制器
 */
@RestController
@RequestMapping("/admin/newbie")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminNewbieController {
    
    private final NewbieProtectionService newbieProtectionService;
    
    /**
     * 获取新手保护配置
     */
    @GetMapping("/config")
    public Result<NewbieConfig> getConfig() {
        NewbieConfig config = newbieProtectionService.getConfig();
        return Result.success(config);
    }
    
    /**
     * 更新新手保护配置
     */
    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody NewbieConfig config) {
        newbieProtectionService.updateConfig(config);
        return Result.success();
    }
    
    /**
     * 获取新手任务列表
     */
    @GetMapping("/tasks")
    public Result<List<NewbieTask>> getTaskList() {
        List<NewbieTask> tasks = newbieProtectionService.getTaskList();
        return Result.success(tasks);
    }
    
    /**
     * 更新新手任务
     */
    @PutMapping("/tasks/{taskId}")
    public Result<Void> updateTask(@PathVariable Long taskId, @RequestBody NewbieTask task) {
        task.setId(taskId);
        newbieProtectionService.updateTask(task);
        return Result.success();
    }
    
    /**
     * 获取待审核的技能认证列表
     */
    @GetMapping("/certifications/pending")
    public Result<Page<SkillCertificationVO>> getPendingCertifications(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<SkillCertificationVO> result = newbieProtectionService.getPendingCertifications(page, size);
        return Result.success(result);
    }
    
    /**
     * 审核技能认证 - 通过
     */
    @PostMapping("/certifications/{certificationId}/approve")
    public Result<Void> approveCertification(@PathVariable Long certificationId) {
        newbieProtectionService.approveCertification(certificationId);
        return Result.success();
    }
    
    /**
     * 审核技能认证 - 拒绝
     */
    @PostMapping("/certifications/{certificationId}/reject")
    public Result<Void> rejectCertification(
        @PathVariable Long certificationId,
        @RequestBody Map<String, String> params
    ) {
        String reason = params.get("reason");
        newbieProtectionService.rejectCertification(certificationId, reason);
        return Result.success();
    }
}
