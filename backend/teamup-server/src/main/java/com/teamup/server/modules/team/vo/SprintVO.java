package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sprint视图对象
 */
@Data
public class SprintVO {
    private Long id;
    private Long teamId;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long createdBy;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 统计信息
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer inProgressTasks;
}
