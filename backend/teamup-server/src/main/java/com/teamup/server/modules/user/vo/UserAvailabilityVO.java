package com.teamup.server.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户可用性响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAvailabilityVO {
    
    /**
     * 是否可用（上人才墙）
     */
    private Boolean isAvailable;
    
    /**
     * 组队意向列表
     */
    private List<String> intentions;
    
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
}
