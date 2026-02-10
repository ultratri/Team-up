package com.teamup.server.modules.search.dto;

import lombok.Data;

/**
 * 搜索项DTO
 */
@Data
public class SearchItemDTO {
    private String type;        // projects, users, teams
    private Long id;
    private String title;
    private String description;
    private String icon;
    private String avatar;
}
