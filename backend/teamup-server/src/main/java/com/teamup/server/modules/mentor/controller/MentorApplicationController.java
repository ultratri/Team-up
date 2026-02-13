package com.teamup.server.modules.mentor.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.api.Result;
import com.teamup.server.modules.mentor.dto.MentorApplicationRequest;
import com.teamup.server.modules.mentor.dto.ReviewApplicationRequest;
import com.teamup.server.modules.mentor.entity.MentorApplication;
import com.teamup.server.modules.mentor.service.MentorApplicationService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 导师申请控制器
 */
@Slf4j
@RestController
@RequestMapping("/mentor-applications")
@RequiredArgsConstructor
public class MentorApplicationController {
    
    private final MentorApplicationService applicationService;
    
    /**
     * 提交导师申请
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<MentorApplication> submitApplication(@RequestBody MentorApplicationRequest request) {
        Long userId = UserContext.getCurrentUserId();
        MentorApplication application = applicationService.submitApplication(userId, request);
        return Result.success(application);
    }
    
    /**
     * 获取当前用户的申请记录
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<MentorApplication> getMyApplication() {
        Long userId = UserContext.getCurrentUserId();
        MentorApplication application = applicationService.getUserApplication(userId);
        return Result.success(application);
    }
    
    /**
     * 检查是否可以申请
     */
    @GetMapping("/can-apply")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> canApply() {
        Long userId = UserContext.getCurrentUserId();
        boolean canApply = applicationService.canApply(userId);
        return Result.success(canApply);
    }
    
    /**
     * 获取申请列表（管理员）
     */
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<Page<MentorApplication>> getApplicationList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        Page<MentorApplication> result = applicationService.getApplicationList(page, size, status);
        return Result.success(result);
    }
    
    /**
     * 审核申请（管理员）
     */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<String> reviewApplication(
            @PathVariable Long id,
            @RequestBody ReviewApplicationRequest request) {
        Long reviewerId = UserContext.getCurrentUserId();
        applicationService.reviewApplication(id, reviewerId, request);
        return Result.success("审核成功");
    }
}
