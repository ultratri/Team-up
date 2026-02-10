package com.teamup.server.modules.competition.vo;

import lombok.Data;

/**
 * 比赛统计数据
 */
@Data
public class CompetitionStatsVO {
    private Long competitionId;
    private long teamCount;
    private long memberCount;
    private long mentorTeamCount;
    /**
     * 浏览量（Redis 计数）
     */
    private long viewCount;
    /**
     * mentorTeamCount / teamCount（百分比 0-100）
     */
    private int mentorCoverageRate;
}

