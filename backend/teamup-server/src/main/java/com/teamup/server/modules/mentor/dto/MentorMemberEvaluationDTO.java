package com.teamup.server.modules.mentor.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 导师成员评价DTO
 */
@Data
public class MentorMemberEvaluationDTO {
    
    /**
     * 被评价成员ID
     */
    @NotNull(message = "成员ID不能为空")
    private Long memberId;
    
    /**
     * 综合评分(0-100)
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 0, message = "评分不能小于0")
    @Max(value = 100, message = "评分不能大于100")
    private Integer score;
    
    /**
     * 技术能力评分(1-5)
     */
    @Min(value = 1, message = "技术能力评分不能小于1")
    @Max(value = 5, message = "技术能力评分不能大于5")
    private Integer technicalAbility;
    
    /**
     * 协作能力评分(1-5)
     */
    @Min(value = 1, message = "协作能力评分不能小于1")
    @Max(value = 5, message = "协作能力评分不能大于5")
    private Integer collaboration;
    
    /**
     * 学习态度评分(1-5)
     */
    @Min(value = 1, message = "学习态度评分不能小于1")
    @Max(value = 5, message = "学习态度评分不能大于5")
    private Integer learningAttitude;
    
    /**
     * 任务完成度评分(1-5)
     */
    @Min(value = 1, message = "任务完成度评分不能小于1")
    @Max(value = 5, message = "任务完成度评分不能大于5")
    private Integer taskCompletion;
    
    /**
     * 评价内容
     */
    private String comment;
}
