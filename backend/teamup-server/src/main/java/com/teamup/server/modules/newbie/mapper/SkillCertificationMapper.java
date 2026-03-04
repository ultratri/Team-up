package com.teamup.server.modules.newbie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.newbie.entity.SkillCertification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 技能认证Mapper
 */
@Mapper
public interface SkillCertificationMapper extends BaseMapper<SkillCertification> {
    
    /**
     * 获取用户已通过的技能认证
     */
    @Select("SELECT * FROM skill_certifications " +
            "WHERE user_id = #{userId} AND status = 'APPROVED'")
    List<SkillCertification> getApprovedCertifications(@Param("userId") Long userId);
}
