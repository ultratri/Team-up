package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目里程碑实体
 */
@Data
@TableName("project_milestones")
public class ProjectMilestone {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String title;
    /**
     * 状态：PLANNED / IN_PROGRESS / DONE
     */
    private String status;
    private LocalDateTime plannedAt;
    private LocalDateTime actualAt;
    private Long ownerId;
    private String remark;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
