package com.teamup.server.modules.team.dto;

import lombok.Data;

@Data
public class TeamCreateRequest {
    private String teamName;
    private Long projectId;
    private Long leaderId;
    /**
     * 团队类型：PROJECT / COMPETITION，不传则默认为 PROJECT
     */
    private String type;
    /**
     * 若为比赛队伍，对应的比赛ID
     */
    private Long competitionId;
    /**
     * 队伍人数上限（可选）
     */
    private Integer maxMembers;
}
