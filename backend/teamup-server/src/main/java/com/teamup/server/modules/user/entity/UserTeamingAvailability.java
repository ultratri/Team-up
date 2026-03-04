package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户组队可用性实体（组队意向）
 */
@Data
@TableName("user_availability")
public class UserTeamingAvailability {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Boolean isAvailable;
    private String intention;
    private String visibility;
    private LocalDate availableFrom;
    private LocalDate availableUntil;
    private Integer weeklyHours;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
