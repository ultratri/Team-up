package com.teamup.server.modules.team.vo;

import lombok.Data;

/**
 * 快捷入口 VO
 */
@Data
public class ShortcutVO {
    private String id;
    private String name;
    private String url;
    private String icon;
    private String color;
    private Integer order;
}
