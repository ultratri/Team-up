package com.teamup.server.modules.project.dto.matching;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 团队找成员：Team -> Users
 * 结构与 MatchRequest 类似，但语义上表示团队而非项目，便于在 Python 侧区分场景与打点。
 */
@Data
public class TeamMatchRequest {

    @JsonProperty("team_id")
    @JsonAlias("teamId")
    private Long teamId;

    /**
     * 团队信息（在匹配服务中作为“项目”维度参与计算）
     */
    private Map<String, Object> team;

    /**
     * 候选人列表
     */
    private List<Map<String, Object>> candidates;
}

