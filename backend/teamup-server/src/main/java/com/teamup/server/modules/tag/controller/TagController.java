package com.teamup.server.modules.tag.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签公共控制器（用户可访问）
 */
@Slf4j
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TagController {
    
    private final TagService tagService;
    
    /**
     * 获取指定分类的热门标签
     */
    @GetMapping("/popular")
    public Result<List<Tag>> getPopularTags(
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<Tag> tags = tagService.getPopularTags(category, limit);
        return Result.success(tags);
    }
    
    /**
     * 获取所有技能标签（用于用户选择）
     */
    @GetMapping("/skills")
    public Result<List<Tag>> getSkillTags() {
        List<Tag> tags = tagService.getPopularTags("SKILL", 100);
        return Result.success(tags);
    }
    
    /**
     * 获取所有兴趣标签（用于用户选择）
     */
    @GetMapping("/interests")
    public Result<List<Tag>> getInterestTags() {
        List<Tag> tags = tagService.getPopularTags("INTEREST", 100);
        return Result.success(tags);
    }
    
    /**
     * 获取所有性格标签（用于用户选择）
     */
    @GetMapping("/personalities")
    public Result<List<Tag>> getPersonalityTags() {
        List<Tag> tags = tagService.getPopularTags("PERSONALITY", 100);
        return Result.success(tags);
    }
    
    /**
     * 获取所有项目类型标签（用于用户选择）
     */
    @GetMapping("/project-types")
    public Result<List<Tag>> getProjectTypeTags() {
        List<Tag> tags = tagService.getPopularTags("PROJECT_TYPE", 100);
        return Result.success(tags);
    }
}
