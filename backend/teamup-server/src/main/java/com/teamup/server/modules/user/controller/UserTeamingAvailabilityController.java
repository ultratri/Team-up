package com.teamup.server.modules.user.controller;

import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.user.dto.UpdateUserTeamingAvailabilityDTO;
import com.teamup.server.modules.user.service.UserTeamingAvailabilityService;
import com.teamup.server.modules.user.vo.UserTeamingAvailabilityVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户组队可用性控制器
 */
@RestController
@RequestMapping("/user/availability")
@RequiredArgsConstructor
public class UserTeamingAvailabilityController {
    
    private final UserTeamingAvailabilityService teamingAvailabilityService;
    
    /**
     * 获取指定用户的组队可用性信息
     * @param userId 用户ID
     * @return 组队可用性信息
     */
    @GetMapping("/{userId}")
    public Result<UserTeamingAvailabilityVO> getUserTeamingAvailability(@PathVariable Long userId) {
        UserTeamingAvailabilityVO vo = teamingAvailabilityService.getUserTeamingAvailability(userId);
        return Result.success(vo);
    }
    
    /**
     * 更新当前用户的组队可用性信息
     * @param dto 更新数据
     * @return 操作结果
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateUserTeamingAvailability(@Valid @RequestBody UpdateUserTeamingAvailabilityDTO dto) {
        Long userId = SecurityUtils.getUserId();
        teamingAvailabilityService.updateUserTeamingAvailability(userId, dto);
        return Result.success(null);
    }
}
