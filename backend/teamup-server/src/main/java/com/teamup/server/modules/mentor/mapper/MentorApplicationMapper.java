package com.teamup.server.modules.mentor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.mentor.entity.MentorApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导师申请Mapper
 */
@Mapper
public interface MentorApplicationMapper extends BaseMapper<MentorApplication> {
}
