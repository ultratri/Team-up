package com.teamup.server.modules.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价实体
 */
@Data
@TableName("evaluations")
public class Evaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    private Long evaluatorId;
    private Long evaluatedId;
    private Integer techContributionScore;
    private Integer collaborationScore;
    private Integer taskCompletionScore;
    private String comment;
    private Boolean isAnonymous;
    private LocalDateTime createdAt;
}
