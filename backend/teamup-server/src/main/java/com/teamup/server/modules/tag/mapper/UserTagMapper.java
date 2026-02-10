package com.teamup.server.modules.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.tag.entity.UserTag;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import com.teamup.server.modules.tag.vo.UserTagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户标签Mapper
 */
@Mapper
public interface UserTagMapper extends BaseMapper<UserTag> {
    
    /**
     * 获取用户的技能标签（带标签名称）
     */
    @Select("SELECT ut.id, ut.user_id, ut.tag_id, t.name as tag_name, " +
            "ut.proficiency_level, ut.is_verified " +
            "FROM user_tags ut " +
            "INNER JOIN tags t ON ut.tag_id = t.id " +
            "WHERE ut.user_id = #{userId} AND t.category = 'SKILL' AND t.status = 'ACTIVE' " +
            "ORDER BY ut.created_at DESC")
    List<UserSkillVO> selectUserSkills(Long userId);
    
    /**
     * 获取用户指定分类的标签（通用）
     */
    @Select("SELECT ut.id, ut.user_id, ut.tag_id, t.name as tag_name, " +
            "ut.proficiency_level, ut.is_verified " +
            "FROM user_tags ut " +
            "INNER JOIN tags t ON ut.tag_id = t.id " +
            "WHERE ut.user_id = #{userId} AND t.category = #{category} AND t.status = 'ACTIVE' " +
            "ORDER BY ut.created_at DESC")
    List<UserTagVO> selectUserTagsByCategory(@Param("userId") Long userId, @Param("category") String category);
}
