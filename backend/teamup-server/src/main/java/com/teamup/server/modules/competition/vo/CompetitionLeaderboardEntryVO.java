package com.teamup.server.modules.competition.vo;

import lombok.Data;

/**
 * 比赛排行榜条目（基于队伍任务完成度）
 */
@Data
public class CompetitionLeaderboardEntryVO {
    private Long teamId;
    private String teamName;
    private Long memberCount;
    private Boolean hasMentor;
    private Long totalTasks;
    private Long doneTasks;
    private int completionRate; // 0-100
    /**
     * 可选：导师/管理员评分（0-100）
     */
    private java.math.BigDecimal score;
    private String comment;
    /**
     * 可选：评分人 ID 与姓名
     */
    private Long scoredBy;
    private String scoredByName;
}

