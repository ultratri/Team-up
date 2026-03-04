package com.teamup.server.modules.project.dto.matching;

import lombok.Data;
import java.util.Map;

/**
 * 用户找项目的匹配结果
 */
@Data
public class UserMatchResult {
    private Long projectId;
    private String projectTitle;
    private Double matchScore;
    private Map<String, Double> breakdown;  // 各维度得分
    private String timeExplanation;         // 时间重叠说明
}
