package com.teamup.server.modules.user.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.user.dto.UserAvailabilityRequest;
import com.teamup.server.modules.user.service.UserAvailabilityService;
import com.teamup.server.modules.user.vo.UserAvailabilityVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户可用性控制器
 * 提供组队意向的查询和更新接口
 */
@RestController
@RequestMapping("/user/availability")
@RequiredArgsConstructor
public class UserAvailabilityController {
    
    private final UserAvailabilityService availabilityService;
    
    /**
     * 获取当前用户的组队意向
     * @return 用户可用性信息
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<UserAvailabilityVO> getCurrentUserAvailability() {
        Long userId = SecurityUtils.getUserId();
        UserAvailabilityVO vo = availabilityService.getUserAvailability(userId);
        return Result.success(vo);
    }
    
    /**
     * 获取指定用户的组队意向
     * @param userId 用户ID
     * @return 用户可用性信息
     */
    @GetMapping("/{userId}")
    public Result<UserAvailabilityVO> getUserAvailability(@PathVariable Long userId) {
        UserAvailabilityVO vo = availabilityService.getUserAvailability(userId);
        return Result.success(vo);
    }
    
    /**
     * 更新当前用户的组队意向
     * @param request 组队意向请求
     * @return 操作结果
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateAvailability(@RequestBody @Valid UserAvailabilityRequest request) {
        Long userId = SecurityUtils.getUserId();
        availabilityService.updateAvailability(userId, request);
        return Result.success(null);
    }
}
