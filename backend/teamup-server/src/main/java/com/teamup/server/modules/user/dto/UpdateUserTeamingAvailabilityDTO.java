package com.teamup.server.modules.user.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 更新用户组队可用性DTO
 */
@Data
public class UpdateUserTeamingAvailabilityDTO {
    private Boolean isAvailable;
    private List<String> intentions;
    private String visibility;
    private LocalDate availableFrom;
    private LocalDate availableUntil;
    private Integer weeklyHours;
    private String notes;
}
