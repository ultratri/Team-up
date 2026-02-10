package com.teamup.server.modules.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.vo.TagUsageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 标签Mapper
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    
    /**
     * 增加标签使用次数
     */
    @Update("UPDATE tags SET usage_count = usage_count + 1 WHERE id = #{tagId}")
    int incrementUsageCount(@Param("tagId") Long tagId);
    
    /**
     * 减少标签使用次数
     */
    @Update("UPDATE tags SET usage_count = usage_count - 1 WHERE id = #{tagId} AND usage_count > 0")
    int decrementUsageCount(@Param("tagId") Long tagId);
    
    /**
     * 查询标签使用统计
     */
    @Select("SELECT " +
            "t.id, " +
            "t.name, " +
            "t.category, " +
            "COUNT(DISTINCT ut.user_id) as user_count, " +
            "COUNT(DISTINCT pt.project_id) as project_count, " +
            "t.usage_count as total_usage " +
            "FROM tags t " +
            "LEFT JOIN user_tags ut ON t.id = ut.tag_id " +
            "LEFT JOIN project_tags pt ON t.id = pt.tag_id " +
            "WHERE t.status = 'ACTIVE' " +
            "GROUP BY t.id, t.name, t.category, t.usage_count " +
            "ORDER BY t.usage_count DESC " +
            "LIMIT #{limit}")
    List<TagUsageVO> selectTagUsageStatistics(@Param("limit") int limit);
}
