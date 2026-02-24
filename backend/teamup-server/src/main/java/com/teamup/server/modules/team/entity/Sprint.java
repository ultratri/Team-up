package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sprint实体类
 * 用于敏捷开发的迭代管理
 */
@Data
@TableName("sprints")
public class Sprint {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;  // PLANNING, IN_PROGRESS, COMPLETED
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
