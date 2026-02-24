package com.teamup.server.modules.team.controller;

import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.team.service.TeamCustomConfigService;
import com.teamup.server.modules.team.vo.TeamCustomConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 团队自定义配置控制器
 */
@RestController
@RequestMapping("/teams/{teamId}/custom-config")
@RequiredArgsConstructor
public class TeamCustomConfigController {
    
    private final TeamCustomConfigService configService;
    
    /**
     * 获取团队自定义配置
     */
    @GetMapping
    public Result<TeamCustomConfigVO> getConfig(@PathVariable Long teamId) {
        Long currentUserId = SecurityUtils.getUserId();
        TeamCustomConfigVO config = configService.getConfig(teamId, currentUserId);
        return Result.success(config);
    }
    
    /**
     * 更新团队自定义配置
     */
    @PutMapping
    public Result<String> updateConfig(
            @PathVariable Long teamId,
            @RequestBody TeamCustomConfigVO config) {
        Long currentUserId = SecurityUtils.getUserId();
        configService.updateConfig(teamId, currentUserId, config);
        return Result.success("配置更新成功");
    }
}
