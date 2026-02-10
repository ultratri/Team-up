package com.teamup.server.modules.user.service;

import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserSkill;
import com.teamup.server.modules.user.entity.UserCredit;
import java.util.List;

/**
 * 用户档案服务接口
 */
public interface ProfileService {
    /**
     * 获取用户档案
     */
    UserProfile getProfileByUserId(Long userId);

    /**
     * 更新用户档案
     */
    void updateProfile(Long userId, UserProfile profile);

    /**
     * 获取用户技能列表
     */
    List<UserSkill> getUserSkills(Long userId);

    /**
     * 添加用户技能
     */
    void addUserSkill(Long userId, UserSkill skill);

    /**
     * 删除用户技能
     */
    void removeUserSkill(Long skillId);

    /**
     * 获取用户信誉信息
     */
    UserCredit getUserCredit(Long userId);
}

