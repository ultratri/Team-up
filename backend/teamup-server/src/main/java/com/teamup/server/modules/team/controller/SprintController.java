package com.teamup.server.modules.team.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.entity.Sprint;
import com.teamup.server.modules.team.service.SprintService;
import com.teamup.server.modules.team.vo.SprintVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Sprint管理控制器
 */
@RestController
@RequestMapping("/sprints")
@RequiredArgsConstructor
public class SprintController {
    
    private final SprintService sprintService;
    
    /**
     * 创建Sprint
     */
    @PostMapping
    public Result<Sprint> createSprint(@RequestBody Sprint sprint) {
        return Result.success(sprintService.createSprint(sprint));
    }
    
    /**
     * 更新Sprint
     */
    @PutMapping("/{id}")
    public Result<Sprint> updateSprint(@PathVariable Long id, @RequestBody Sprint sprint) {
        sprint.setId(id);
        return Result.success(sprintService.updateSprint(sprint));
    }
    
    /**
     * 删除Sprint
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSprint(@PathVariable Long id) {
        sprintService.deleteSprint(id);
        return Result.success();
    }
    
    /**
     * 获取团队的所有Sprint
     */
    @GetMapping("/team/{teamId}")
    public Result<List<SprintVO>> getTeamSprints(@PathVariable Long teamId) {
        return Result.success(sprintService.getTeamSprints(teamId));
    }
    
    /**
     * 获取Sprint详情
     */
    @GetMapping("/{id}")
    public Result<SprintVO> getSprintDetail(@PathVariable Long id) {
        return Result.success(sprintService.getSprintDetail(id));
    }
    
    /**
     * 开始Sprint
     */
    @PostMapping("/{id}/start")
    public Result<Void> startSprint(@PathVariable Long id) {
        sprintService.startSprint(id);
        return Result.success();
    }
    
    /**
     * 完成Sprint
     */
    @PostMapping("/{id}/complete")
    public Result<Void> completeSprint(@PathVariable Long id) {
        sprintService.completeSprint(id);
        return Result.success();
    }
}
