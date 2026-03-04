package com.teamup.server.modules.project.dto.matching;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class MatchFeedbackRequest {
    @JsonProperty("project_id")
    @JsonAlias("projectId")
    private Long projectId;

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Long userId;

    @JsonProperty("project_type")
    @JsonAlias("projectType")
    private String projectType;

    private String event;
    private String source;

    @JsonProperty("event_time")
    @JsonAlias("eventTime")
    private Long eventTime;

    private Map<String, Double> breakdown;
}
