package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户时间段实体（用于匹配功能）
 */
@Data
@TableName("user_time_slots")
public class UserAvailability {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Integer dayOfWeek;  // 星期几(1-7,1表示周一)
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
