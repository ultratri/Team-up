package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.UserProjectHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户项目履历Mapper
 */
@Mapper
public interface UserProjectHistoryMapper extends BaseMapper<UserProjectHistory> {
}
