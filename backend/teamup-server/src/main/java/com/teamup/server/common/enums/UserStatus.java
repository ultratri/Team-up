package com.teamup.server.common.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    ACTIVE("正常"),
    INACTIVE("未激活"),
    BANNED("已封禁");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
