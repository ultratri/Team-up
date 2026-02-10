package com.teamup.server.modules.ecosystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.ecosystem.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 资源Mapper接口
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {
    
    /**
     * 增加浏览量
     */
    @Update("UPDATE resources SET views = views + 1 WHERE id = #{id}")
    void incrementViews(@Param("id") Long id);
    
    /**
     * 增加点赞数
     */
    @Update("UPDATE resources SET likes = likes + 1 WHERE id = #{id}")
    void incrementLikes(@Param("id") Long id);
    
    /**
     * 减少点赞数
     */
    @Update("UPDATE resources SET likes = likes - 1 WHERE id = #{id} AND likes > 0")
    void decrementLikes(@Param("id") Long id);
}
