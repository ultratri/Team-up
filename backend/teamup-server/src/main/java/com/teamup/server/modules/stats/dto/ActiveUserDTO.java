package com.teamup.server.modules.stats.dto;

import lombok.Data;

/**
 * 活跃用户DTO
 */
@Data
public class ActiveUserDTO {
    private Long userId;
    private String name;
    private Long count;
}
