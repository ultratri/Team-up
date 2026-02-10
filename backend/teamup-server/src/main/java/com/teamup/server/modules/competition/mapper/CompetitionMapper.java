package com.teamup.server.modules.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.competition.entity.Competition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛 Mapper
 */
@Mapper
public interface CompetitionMapper extends BaseMapper<Competition> {

    /**
     * 查询报名即将截止的已发布比赛
     */
    @Select("SELECT * FROM competitions WHERE status = 'PUBLISHED' AND signup_end_at IS NOT NULL AND signup_end_at BETWEEN #{start} AND #{end} ORDER BY signup_end_at ASC")
    List<Competition> selectSignupEndingSoon(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查询即将开始的已发布比赛
     */
    @Select("SELECT * FROM competitions WHERE status = 'PUBLISHED' AND start_at IS NOT NULL AND start_at BETWEEN #{start} AND #{end} ORDER BY start_at ASC")
    List<Competition> selectStartingSoon(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

