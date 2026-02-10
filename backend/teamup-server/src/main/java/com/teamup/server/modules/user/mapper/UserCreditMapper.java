package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.UserCredit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信誉Mapper
 */
@Mapper
public interface UserCreditMapper extends BaseMapper<UserCredit> {
}

