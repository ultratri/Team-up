package com.teamup.server.modules.project.dto.matching;

import lombok.Data;

@Data
public class MatchResult {
    private Long user_id;
    private String username;
    private Double match_score;
    private Double skill_match;
    private Double semantic_match;
    private String credit_level;
}
