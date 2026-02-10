package com.teamup.server.modules.mentor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 导师绩效实体
 */
@Data
@TableName("mentor_performance")
public class MentorPerformance {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 导师ID
     */
    private Long mentorId;
    
    /**
     * 总学员数
     */
    private Integer totalMentees;
    
    /**
     * 当前活跃学员数
     */
    private Integer activeMentees;
    
    /**
     * 已完成学员数
     */
    private Integer completedMentees;
    
    /**
     * 成功培养学员数（信誉分>60）
     */
    private Integer successfulMentees;
    
    /**
     * 学员平均信誉分
     */
    private BigDecimal averageMenteeScore;
    
    /**
     * 累计奖励积分
     */
    private Integer totalRewardPoints;
    
    /**
     * 导师评分（0-5）
     */
    private BigDecimal rating;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
