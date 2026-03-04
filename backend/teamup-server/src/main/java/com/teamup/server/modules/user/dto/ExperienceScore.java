package com.teamup.server.modules.user.dto;

import lombok.Data;
import java.util.Map;

/**
 * 经验分数DTO
 * 用于匹配算法的经验维度计算
 */
@Data
public class ExperienceScore {
    
    /**
     * 总分（0-100）
     */
    private Double totalScore;
    
    /**
     * 是否基于系统验证的数据
     */
    private Boolean isVerified;
    
    /**
     * 已完成项目数量
     */
    private Integer completedProjects;
    
    /**
     * 分项得分
     */
    private Map<String, Double> breakdown;
    
    /**
     * 平均项目评分（1-5）
     */
    private Double avgProjectScore;
    
    /**
     * 领导项目数量
     */
    private Integer leaderProjects;
    
    /**
     * 项目类型多样性
     */
    private Integer projectTypeDiversity;
}
