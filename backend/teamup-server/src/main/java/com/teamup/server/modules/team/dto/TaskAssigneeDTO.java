package com.teamup.server.modules.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务负责人DTO
 * Requirements: 2.1, 10.1, 10.2
 */
@Data
public class TaskAssigneeDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 分配时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime assignedAt;
}
