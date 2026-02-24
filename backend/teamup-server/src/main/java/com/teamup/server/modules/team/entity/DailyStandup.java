package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日站会记录实体类
 */
@Data
@TableName("daily_standups")
public class DailyStandup {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    private Long sprintId;
    private Long userId;
    private LocalDate standupDate;
    private String yesterdayWork;
    private String todayPlan;
    private String blockers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
