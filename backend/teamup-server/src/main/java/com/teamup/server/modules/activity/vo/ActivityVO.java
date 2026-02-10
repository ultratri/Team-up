package com.teamup.server.modules.activity.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 活动记录VO
 */
@Data
public class ActivityVO {
    private Long id;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String activityType;
    private String action;
    private String detail;
    private LocalDateTime createdAt;
}
