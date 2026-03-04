package com.teamup.server.modules.user.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用户组队可用性VO
 */
@Data
public class UserTeamingAvailabilityVO {
    private Long id;
    private Long userId;
    private Boolean isAvailable;
    private String intention;  // 数据库字段
    private String visibility;
    private LocalDate availableFrom;
    private LocalDate availableUntil;
    private Integer weeklyHours;
    private String notes;
    
    /**
     * 获取intentions列表（前端需要的格式）
     */
    public List<String> getIntentions() {
        if (intention == null || intention.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(intention.split(","));
    }
    
    /**
     * 设置intentions列表（前端传入的格式）
     */
    public void setIntentions(List<String> intentions) {
        if (intentions == null || intentions.isEmpty()) {
            this.intention = null;
        } else {
            this.intention = String.join(",", intentions);
        }
    }
}
