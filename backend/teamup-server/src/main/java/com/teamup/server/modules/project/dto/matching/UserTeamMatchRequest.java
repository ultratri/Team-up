package com.teamup.server.modules.project.dto.matching;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserTeamMatchRequest {
    private Long userId;
    private Map<String, Object> user;
    private List<Map<String, Object>> teams;
}
