package com.teamup.server.modules.project.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MilestoneVO {
    private Long id;
    private Long projectId;
    private String title;
    private String status;
    private LocalDateTime plannedAt;
    private LocalDateTime actualAt;
    private Long ownerId;
    private String ownerName;
    private String remark;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
