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
    private Long evaluatorId;  // 评价者ID
    private Long evaluatedId;  // 被评价者ID
    private Integer techContributionScore;  // 技术贡献分（1-5）
    private Integer collaborationScore;  // 协作能力分（1-5）
    private Integer taskCompletionScore;  // 任务完成分（1-5）
    private String comment;
    private Boolean isAnonymous;
    private LocalDateTime createdAt;
}
