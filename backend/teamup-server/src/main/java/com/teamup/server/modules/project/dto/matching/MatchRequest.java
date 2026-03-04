package com.teamup.server.modules.project.dto.matching;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MatchRequest {
    @JsonProperty("project_id")
    @JsonAlias("projectId")
    private Long projectId;

    private Map<String, Object> project;
    private List<Map<String, Object>> candidates;
}
