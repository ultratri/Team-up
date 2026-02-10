package com.teamup.server.modules.newbie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新手任务实体
 */
@Data
@TableName("newbie_tasks")
public class NewbieTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taskName;
    private String taskDescription;
    private String taskType;               // COMPLETE_PROFILE, SKILL_CERTIFICATION, FIRST_PROJECT, FIRST_EVALUATION
    private Integer rewardPoints;
    private Boolean isActive;
    private Integer displayOrder;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
