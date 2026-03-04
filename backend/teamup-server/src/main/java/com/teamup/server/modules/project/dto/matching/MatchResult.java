package com.teamup.server.modules.project.dto.matching;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class MatchResult {
    @JsonProperty("user_id")
    private Long userId;
    
    private String username;
    
    // Python返回的是score，不是match_score
    @JsonProperty("score")
    private Double score;
    
    // Python返回的是breakdown（各维度得分的Map）
    @JsonProperty("breakdown")
    private Map<String, Double> breakdown;

    @JsonProperty("time_explanation")
    private String timeExplanation;

    /**
     * 为前端展示的推荐理由（由后端根据各维度得分生成的简短说明）
     */
    private String matchReason;

    /**
     * 详细可解释信息
     */
    private String explainSummary;                 // 自然语言总结
    private java.util.List<String> strengths;      // 拉高因素
    private java.util.List<String> weaknesses;     // 拉低因素
    private java.util.List<String> improvementTips;// 提升建议

    /**
     * 置信度与风险
     */
    private Double confidence;     // 0-1
    private String confidenceLevel;// HIGH/MEDIUM/LOW
    private String riskLevel;      // LOW/MEDIUM/HIGH

    /**
     * 补充字段
     */
    private Integer creditScore;   // 信誉分
    
    // 补充的用户详细信息
    private String department;  // 学院
    private String major;       // 专业
    private Integer grade;      // 年级
    private String bio;         // 个人简介
    
    // 为了兼容前端，添加matchScore的getter（返回score的值）
    public Double getMatchScore() {
        return this.score;
    }
    
    // 从breakdown中提取技能匹配度
    public Double getSkillMatch() {
        return breakdown != null ? breakdown.get("skill") : null;
    }
}
