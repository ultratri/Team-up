package com.teamup.server.modules.evaluation.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价VO
 */
@Data
public class EvaluationVO {
    private Long id;
    private Long evaluatorId;
    private String evaluatorName;  // 匿名时为 null
    private Long evaluatedId;
    private String evaluatedName;
    private Integer techContributionScore;
    private Integer collaborationScore;
    private Integer taskCompletionScore;
    private String comment;
    private Boolean isAnonymous;
    private LocalDateTime createdAt;
}
