package com.teamup.server.common.enums;

/**
 * 项目状态枚举
 */
public enum ProjectStatus {
    DRAFT("草稿"),
    RECRUITING("招募中"),
    IN_PROGRESS("进行中"),
    PENDING_REVIEW("待审核"),
    COMPLETED("已完成"),
    ARCHIVED("已归档");

    private final String description;

    ProjectStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
