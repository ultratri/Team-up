package com.teamup.server.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.message.entity.ConversationMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话成员Mapper
 */
@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {
}
