package com.teamup.server.modules.team.vo;

import lombok.Data;

/**
 * 分组链接 VO
 */
@Data
public class GroupLinkVO {
    private String id;
    private String name;
    private String url;
    private String icon;
    private String description;
}
