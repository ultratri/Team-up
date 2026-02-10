package com.teamup.server.modules.tag.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.tag.dto.CreateTagDTO;
import com.teamup.server.modules.tag.dto.MergeTagDTO;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.service.TagService;
import com.teamup.server.modules.tag.vo.TagUsageVO;
import com.teamup.server.modules.user.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-标签管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminTagController {
    
    private final TagService tagService;
    
    /**
     * 创建标签
     */
    @PostMapping
    public Result<Long> createTag(@Valid @RequestBody CreateTagDTO dto) {
        Long adminId = UserContext.getCurrentUserId();
        Long tagId = tagService.createTag(adminId, dto);
        return Result.success(tagId);
    }
    
    /**
     * 更新标签
     */
    @PutMapping("/{tagId}")
    public Result<Void> updateTag(
        @PathVariable Long tagId,
        @Valid @RequestBody CreateTagDTO dto
    ) {
        tagService.updateTag(tagId, dto);
        return Result.success();
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/{tagId}")
    public Result<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return Result.success();
    }
    
    /**
     * 合并标签
     */
    @PostMapping("/merge")
    public Result<Void> mergeTags(@Valid @RequestBody MergeTagDTO dto) {
        tagService.mergeTags(dto);
        return Result.success();
    }
    
    /**
     * 查询标签列表
     */
    @GetMapping
    public Result<Page<Tag>> listTags(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Tag.TagCategory category,
        @RequestParam(required = false) String keyword
    ) {
        Page<Tag> result = tagService.listTags(page, size, category, keyword);
        return Result.success(result);
    }
    
    /**
     * 查询标签详情
     */
    @GetMapping("/{tagId}")
    public Result<Tag> getTagDetail(@PathVariable Long tagId) {
        Tag tag = tagService.getTagDetail(tagId);
        return Result.success(tag);
    }
    
    /**
     * 查询标签使用统计
     */
    @GetMapping("/statistics")
    public Result<List<TagUsageVO>> getTagUsageStatistics(
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<TagUsageVO> statistics = tagService.getTagUsageStatistics(limit);
        return Result.success(statistics);
    }
}
