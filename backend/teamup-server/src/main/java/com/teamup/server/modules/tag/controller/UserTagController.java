package com.teamup.server.modules.tag.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.tag.dto.AddUserSkillDTO;
import com.teamup.server.modules.tag.dto.AddUserTagDTO;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.service.UserTagService;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import com.teamup.server.modules.tag.vo.UserTagVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户标签控制器
 */
@Slf4j
@RestController
@RequestMapping("/user-tags")
@RequiredArgsConstructor
public class UserTagController {
    
    private final UserTagService userTagService;
    
    /**
     * 获取用户的所有标签
     */
    @GetMapping("/user/{userId}")
    public Result<List<UserTagVO>> getUserAllTags(@PathVariable Long userId) {
        List<UserTagVO> tags = userTagService.getUserAllTags(userId);
        return Result.success(tags);
    }
    
    /**
     * 获取用户的技能标签
     */
    @GetMapping("/{userId}/skills")
    public Result<List<UserSkillVO>> getUserSkills(@PathVariable Long userId) {
        List<UserSkillVO> skills = userTagService.getUserSkills(userId);
        return Result.success(skills);
    }
    
    /**
     * 添加用户技能标签
     */
    @PostMapping("/{userId}/skills")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> addUserSkill(
        @PathVariable Long userId,
        @Valid @RequestBody AddUserSkillDTO dto
    ) {
        userTagService.addUserSkill(userId, dto);
        return Result.success();
    }
    
    /**
     * 删除用户技能标签
     */
    @DeleteMapping("/skills/{userTagId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> removeUserSkill(@PathVariable Long userTagId) {
        userTagService.removeUserSkill(userTagId);
        return Result.success();
    }
    
    /**
     * 获取用户指定分类的标签（通用）
     */
    @GetMapping("/{userId}/tags/{category}")
    public Result<List<UserTagVO>> getUserTagsByCategory(
        @PathVariable Long userId,
        @PathVariable String category
    ) {
        List<UserTagVO> tags = userTagService.getUserTagsByCategory(userId, category.toUpperCase());
        return Result.success(tags);
    }
    
    /**
     * 添加用户标签（通用）
     */
    @PostMapping("/{userId}/tags")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> addUserTag(
        @PathVariable Long userId,
        @Valid @RequestBody AddUserTagDTO dto
    ) {
        userTagService.addUserTag(userId, dto);
        return Result.success();
    }
    
    /**
     * 删除用户标签（通用）
     */
    @DeleteMapping("/tags/{userTagId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> removeUserTag(@PathVariable Long userTagId) {
        userTagService.removeUserTag(userTagId);
        return Result.success();
    }
}
