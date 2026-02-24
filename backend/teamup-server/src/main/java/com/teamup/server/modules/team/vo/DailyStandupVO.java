package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日站会视图对象
 */
@Data
public class DailyStandupVO {
    private Long id;
    private Long teamId;
    private Long sprintId;
    private String sprintName;
    private Long userId;
    private String userName;
    private String userAvatar;
    private LocalDate standupDate;
    private String yesterdayWork;
    private String todayPlan;
    private String blockers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
