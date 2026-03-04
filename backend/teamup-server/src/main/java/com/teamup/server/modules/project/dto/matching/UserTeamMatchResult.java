package com.teamup.server.modules.project.dto.matching;

import lombok.Data;

import java.util.Map;

@Data
public class UserTeamMatchResult {
    private Long teamId;
    private Double matchScore;
    private Map<String, Double> breakdown;
    private String matchReason;
}
