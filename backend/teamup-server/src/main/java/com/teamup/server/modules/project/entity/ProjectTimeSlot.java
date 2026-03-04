package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 项目时间段需求实体
 */
@Data
@TableName("project_time_slots")
public class ProjectTimeSlot {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Integer dayOfWeek;   // 1-7, 1=周一
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
}
