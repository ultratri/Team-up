package com.teamup.server.modules.competition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 比赛队伍评分实体
 */
@Data
@TableName("team_competition_scores")
public class TeamCompetitionScore {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long competitionId;
    private Long teamId;
    private BigDecimal score;
    private String comment;
    private Long scoredBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

