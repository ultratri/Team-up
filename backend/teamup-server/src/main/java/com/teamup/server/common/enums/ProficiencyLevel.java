package com.teamup.server.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 技能熟练度等级枚举
 * 统一定义系统中所有技能等级相关的常量
 * 
 * @author TeamUp
 * @since 2026-03-03
 */
@Getter
@AllArgsConstructor
public enum ProficiencyLevel {
    
    /**
     * 入门级别 - 刚开始学习，了解基础概念
     */
    BEGINNER("BEGINNER", "入门", 0.4),
    
    /**
     * 熟练级别 - 能够独立完成常规任务
     */
    INTERMEDIATE("INTERMEDIATE", "熟练", 0.7),
    
    /**
     * 高级级别 - 能够处理复杂问题，有深入理解
     */
    ADVANCED("ADVANCED", "高级", 0.85),
    
    /**
     * 精通级别 - 专家水平，能够指导他人
     */
    EXPERT("EXPERT", "精通", 1.0);
    
    /**
     * 等级代码
     */
    private final String code;
    
    /**
     * 等级名称
     */
    private final String name;
    
    /**
     * 匹配分数权重 (0.0-1.0)
     */
    private final double score;
    
    /**
     * 根据代码获取枚举
     */
    public static ProficiencyLevel fromCode(String code) {
        if (code == null) {
            return INTERMEDIATE; // 默认返回熟练级别
        }
        for (ProficiencyLevel level : values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return INTERMEDIATE; // 未找到时返回默认值
    }
    
    /**
     * 根据代码获取分数
     */
    public static double getScore(String code) {
        return fromCode(code).getScore();
    }
    
    /**
     * 根据代码获取名称
     */
    public static String getName(String code) {
        return fromCode(code).getName();
    }
}
