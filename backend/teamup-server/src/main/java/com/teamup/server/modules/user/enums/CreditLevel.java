package com.teamup.server.modules.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户信誉等级枚举
 */
@Getter
@AllArgsConstructor
public enum CreditLevel {
    NEWBIE("NEWBIE", "萌新", 1.0),
    RELIABLE("RELIABLE", "靠谱", 1.2),
    EXCELLENT("EXCELLENT", "优秀", 1.5),
    OUTSTANDING("OUTSTANDING", "杰出", 2.0);

    private final String code;
    private final String description;
    private final double multiplier;

    public static CreditLevel fromCode(String code) {
        for (CreditLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return NEWBIE;
    }
}
