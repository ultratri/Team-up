package com.teamup.server.modules.ecosystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.ecosystem.dto.MomentCreateDTO;
import com.teamup.server.modules.ecosystem.dto.ResourceCreateDTO;
import com.teamup.server.modules.ecosystem.service.EcosystemService;
import com.teamup.server.modules.ecosystem.service.MomentService;
import com.teamup.server.modules.ecosystem.service.ResourceService;
import com.teamup.server.modules.ecosystem.vo.EcosystemStatsVO;
import com.teamup.server.modules.ecosystem.vo.MomentVO;
import com.teamup.server.modules.ecosystem.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 生态广场控制器
 */
@RestController
@RequestMapping("/api/ecosystem")
@RequiredArgsConstructor
public class EcosystemController {
    
    private final EcosystemService ecosystemService;
    private final ResourceService resourceService;
    private final MomentService momentService;
    
    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Result<EcosystemStatsVO> getStats() {
        return Result.success(ecosystemService.getStats());
    }
    
    // ==================== 资源相关接口 ====================
    
    /**
     * 创建资源
     */
    @PostMapping("/resources")
    public Result<Long> createResource(@RequestBody ResourceCreateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        Long resourceId = resourceService.createResource(dto, userId);
        return Result.success(resourceId);
    }
    
    /**
     * 获取资源列表
     */
    @GetMapping("/resources")
    public Result<Page<ResourceVO>> getResourceList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "latest") String sortBy) {
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 未登录用户也可以浏览
        }
        Page<ResourceVO> result = resourceService.getResourceList(page, size, type, sortBy, userId);
        return Result.success(result);
    }
    
    /**
     * 获取资源详情
     */
    @GetMapping("/resources/{id}")
    public Result<ResourceVO> getResourceDetail(@PathVariable Long id) {
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 未登录用户也可以浏览
        }
        ResourceVO resource = resourceService.getResourceDetail(id, userId);
        return Result.success(resource);
    }
    
    /**
     * 点赞资源
     */
    @PostMapping("/resources/{id}/like")
    public Result<Void> likeResource(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        resourceService.likeResource(id, userId);
        return Result.success(null);
    }
    
    /**
     * 取消点赞资源
     */
    @DeleteMapping("/resources/{id}/like")
    public Result<Void> unlikeResource(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        resourceService.unlikeResource(id, userId);
        return Result.success(null);
    }
    
    /**
     * 删除资源
     */
    @DeleteMapping("/resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        resourceService.deleteResource(id, userId);
        return Result.success(null);
    }
    
    // ==================== 动态相关接口 ====================
    
    /**
     * 创建动态
     */
    @PostMapping("/moments")
    public Result<Long> createMoment(@RequestBody MomentCreateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        Long momentId = momentService.createMoment(dto, userId);
        return Result.success(momentId);
    }
    
    /**
     * 获取动态列表
     */
    @GetMapping("/moments")
    public Result<Page<MomentVO>> getMomentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type) {
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 未登录用户也可以浏览
        }
        Page<MomentVO> result = momentService.getMomentList(page, size, type, userId);
        return Result.success(result);
    }
    
    /**
     * 点赞动态
     */
    @PostMapping("/moments/{id}/like")
    public Result<Void> likeMoment(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        momentService.likeMoment(id, userId);
        return Result.success(null);
    }
    
    /**
     * 取消点赞动态
     */
    @DeleteMapping("/moments/{id}/like")
    public Result<Void> unlikeMoment(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        momentService.unlikeMoment(id, userId);
        return Result.success(null);
    }
    
    /**
     * 删除动态
     */
    @DeleteMapping("/moments/{id}")
    public Result<Void> deleteMoment(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        momentService.deleteMoment(id, userId);
        return Result.success(null);
    }
}
