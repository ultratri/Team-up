package com.teamup.server.modules.user.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户可用性请求DTO
 */
@Data
public class UserAvailabilityRequest {
    
    /**
     * 是否可用（上人才墙）
     */
    @NotNull(message = "是否可用不能为空")
    private Boolean isAvailable;
    
    /**
     * 组队意向列表
     */
    private List<String> intentions;
    
    /**
     * 可见范围：PUBLIC,PROJECT_CREATOR,MENTOR
     */
    @Pattern(regexp = "PUBLIC|PROJECT_CREATOR|MENTOR", 
             message = "可见范围必须是PUBLIC、PROJECT_CREATOR或MENTOR")
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
    @Min(value = 0, message = "每周小时数不能为负")
    @Max(value = 168, message = "每周小时数不能超过168")
    private Integer weeklyHours;
    
    /**
     * 备注说明
     */
    @Size(max = 200, message = "备注不能超过200字")
    private String notes;
}
