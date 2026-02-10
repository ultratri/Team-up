package com.teamup.server.modules.evaluation.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 评价提交DTO
 */
@Data
public class EvaluationDTO {
    @NotNull(message = "被评价者ID不能为空")
    private Long evaluatedId;
    
    @NotNull(message = "技术贡献分数不能为空")
    @Min(value = 1, message = "技术贡献分数必须在1-5之间")
    @Max(value = 5, message = "技术贡献分数必须在1-5之间")
    private Integer techContributionScore;
    
    @NotNull(message = "协作能力分数不能为空")
    @Min(value = 1, message = "协作能力分数必须在1-5之间")
    @Max(value = 5, message = "协作能力分数必须在1-5之间")
    private Integer collaborationScore;
    
    @NotNull(message = "任务完成分数不能为空")
    @Min(value = 1, message = "任务完成分数必须在1-5之间")
    @Max(value = 5, message = "任务完成分数必须在1-5之间")
    private Integer taskCompletionScore;
    
    private String comment;
    
    private Boolean isAnonymous;
}
