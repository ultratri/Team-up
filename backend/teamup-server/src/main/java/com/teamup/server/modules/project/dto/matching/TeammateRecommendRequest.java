package com.teamup.server.modules.project.dto.matching;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 智能组队推荐：System -> Users（为指定用户推荐可组队的同学）
 */
@Data
public class TeammateRecommendRequest {

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Long userId;

    /**
     * 当前用户的画像数据（与 UserMatchRequest.user 结构相同）
     */
    private Map<String, Object> user;

    /**
     * 候选用户列表（与项目招人/团队找人成员候选结构相同）
     */
    private List<Map<String, Object>> candidates;
}

