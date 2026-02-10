package com.teamup.server.modules.stats.dto;

import lombok.Data;

/**
 * 趋势数据DTO
 */
@Data
public class TrendDataDTO {
    private String date;
    private Long count;
}
