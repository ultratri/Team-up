package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Sprint回顾会议记录实体类
 */
@Data
@TableName("sprint_retrospectives")
public class SprintRetrospective {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sprintId;
    private Long teamId;
    private String whatWentWell;
    private String whatToImprove;
    private String actionItems;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
