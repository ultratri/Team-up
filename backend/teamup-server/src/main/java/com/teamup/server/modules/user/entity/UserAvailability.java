package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户可用性/组队意向实体
 */
@Data
@TableName("user_availability")
public class UserAvailability {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 是否可用（上人才墙）
     */
    private Boolean isAvailable;
    
    /**
     * 意向（逗号分隔）：JOIN_PROJECT,FIND_TEAMMATES,FIND_MENTOR,HELP_NEWBIE
     */
    private String intention;
    
    /**
     * 可见范围：PUBLIC,PROJECT_CREATOR,MENTOR
     */
    private String visibility;
    
    /**
     * 可用开始时间
     */
    private LocalDate availableFrom;
    
    /**
     * 可用结束时间
     */
    private LocalDate availableUntil;
    
    /**
     * 每周可投入小时数
     */
    private Integer weeklyHours;
    
    /**
     * 备注说明
     */
    private String notes;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
