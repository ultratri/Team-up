package com.teamup.server.modules.ecosystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.ecosystem.entity.Moment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 动态Mapper接口
 */
@Mapper
public interface MomentMapper extends BaseMapper<Moment> {
    
    /**
     * 增加点赞数
     */
    @Update("UPDATE moments SET likes = likes + 1 WHERE id = #{id}")
    void incrementLikes(@Param("id") Long id);
    
    /**
     * 减少点赞数
     */
    @Update("UPDATE moments SET likes = likes - 1 WHERE id = #{id} AND likes > 0")
    void decrementLikes(@Param("id") Long id);
    
    /**
     * 增加评论数
     */
    @Update("UPDATE moments SET comments = comments + 1 WHERE id = #{id}")
    void incrementComments(@Param("id") Long id);
}
