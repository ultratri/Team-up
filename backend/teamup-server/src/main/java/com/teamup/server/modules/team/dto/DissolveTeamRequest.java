package com.teamup.server.modules.team.dto;

import lombok.Data;

/**
 * 解散团队请求
 */
@Data
public class DissolveTeamRequest {
    /**
     * 解散原因
     */
    private String reason;
}
