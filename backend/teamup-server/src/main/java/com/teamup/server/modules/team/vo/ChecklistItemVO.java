package com.teamup.server.modules.team.vo;

import lombok.Data;

/**
 * 检查清单项 VO
 */
@Data
public class ChecklistItemVO {
    private String id;
    private String title;
    private String description;
    private Integer order;
    private Boolean required;
}
