package com.teamup.server.modules.project.dto.matching;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MatchRequest {
    private Long project_id;
    private Map<String, Object> project;
    private List<Map<String, Object>> candidates;
}
