package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务负责人实体
 * Requirements: 1.1, 3.1, 4.1
 */
@Data
@TableName("task_assignees")
public class TaskAssignee {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;
}
