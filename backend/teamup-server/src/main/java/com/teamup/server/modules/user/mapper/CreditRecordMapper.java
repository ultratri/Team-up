package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.CreditRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信誉变更记录Mapper
 */
@Mapper
public interface CreditRecordMapper extends BaseMapper<CreditRecord> {
}
