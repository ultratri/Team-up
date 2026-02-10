package com.teamup.server.modules.user.controller;

import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserSkill;
import com.teamup.server.modules.user.entity.UserCredit;
import com.teamup.server.modules.user.service.ProfileService;
import com.teamup.server.modules.tag.service.UserTagService;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import com.teamup.server.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户档案控制器
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserTagService userTagService;

    /**
     * 获取用户档案
     */
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public Result<UserProfile> getProfile(@PathVariable Long userId) {
        UserProfile profile = profileService.getProfileByUserId(userId);
        return Result.success(profile);
    }

    /**
     * 更新用户档案
     */
    @PutMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateProfile(
            @PathVariable Long userId,
            @RequestBody UserProfile profile
    ) {
        profileService.updateProfile(userId, profile);
        return Result.success();
    }

    /**
     * 获取用户技能列表（使用新的标签系统）
     */
    @GetMapping("/{userId}/skills")
    @PreAuthorize("isAuthenticated()")
    public Result<List<UserSkillVO>> getUserSkills(@PathVariable Long userId) {
        List<UserSkillVO> skills = userTagService.getUserSkills(userId);
        return Result.success(skills);
    }

    /**
     * 添加用户技能
     */
    @PostMapping("/{userId}/skills")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> addUserSkill(
            @PathVariable Long userId,
            @RequestBody UserSkill skill
    ) {
        profileService.addUserSkill(userId, skill);
        return Result.success();
    }

    /**
     * 删除用户技能（使用新的标签系统）
     */
    @DeleteMapping("/skills/{skillId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> removeUserSkill(@PathVariable Long skillId) {
        userTagService.removeUserSkill(skillId);
        return Result.success();
    }

    /**
     * 获取用户信誉信息
     */
    @GetMapping("/{userId}/credit")
    @PreAuthorize("isAuthenticated()")
    public Result<UserCredit> getUserCredit(@PathVariable Long userId) {
        UserCredit credit = profileService.getUserCredit(userId);
        return Result.success(credit);
    }
}

