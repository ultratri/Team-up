package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.UserSkill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户技能Mapper
 */
@Mapper
public interface UserSkillMapper extends BaseMapper<UserSkill> {
}

