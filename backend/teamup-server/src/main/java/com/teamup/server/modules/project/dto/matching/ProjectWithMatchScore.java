package com.teamup.server.modules.project.dto.matching;

import com.teamup.server.modules.project.entity.Project;
import lombok.Data;
import java.util.Map;

/**
 * 带匹配分数的项目DTO
 */
@Data
public class ProjectWithMatchScore {
    private Project project;
    private Double matchScore;  // 总匹配分数
    private Map<String, Double> breakdown;  // 各维度得分详情
    private String matchReason;  // 匹配原因说明
    private String timeExplanation; // 时间重叠说明
}
