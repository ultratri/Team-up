package com.teamup.server.modules.mentor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导师对团队成员的评价实体
 */
@Data
@TableName("mentor_member_evaluations")
public class MentorMemberEvaluation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 团队ID
     */
    private Long teamId;
    
    /**
     * 导师ID
     */
    private Long mentorId;
    
    /**
     * 被评价成员ID
     */
    private Long memberId;
    
    /**
     * 综合评分(0-100)
     */
    private Integer score;
    
    /**
     * 技术能力评分(1-5)
     */
    private Integer technicalAbility;
    
    /**
     * 协作能力评分(1-5)
     */
    private Integer collaboration;
    
    /**
     * 学习态度评分(1-5)
     */
    private Integer learningAttitude;
    
    /**
     * 任务完成度评分(1-5)
     */
    private Integer taskCompletion;
    
    /**
     * 评价内容
     */
    private String comment;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
