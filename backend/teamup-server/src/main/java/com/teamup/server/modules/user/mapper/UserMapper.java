package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 查询活跃用户（按最后登录时间降序）
     * @param limit 查询数量
     * @return 活跃用户列表
     */
    @Select("SELECT * FROM users ORDER BY updated_at DESC LIMIT #{limit}")
    List<User> selectActiveUsers(int limit);

    /**
     * 查询活跃用户ID（用于批量通知）
     */
    @Select("SELECT id FROM users WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT #{limit}")
    List<Long> selectActiveUserIds(int limit);
}

