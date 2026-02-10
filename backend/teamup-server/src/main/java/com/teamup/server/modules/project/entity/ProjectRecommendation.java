package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目匹配推荐结果实体
 */
@Data
@TableName("project_recommendations")
public class ProjectRecommendation {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    private Long userId;
    
    private BigDecimal matchScore;
    private BigDecimal skillMatchScore;
    private BigDecimal semanticMatchScore;
    private String recommendReason;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
