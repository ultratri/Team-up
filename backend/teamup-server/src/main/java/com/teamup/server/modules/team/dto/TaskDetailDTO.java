package com.teamup.server.modules.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务详情DTO
 * Requirements: 2.1, 10.1, 10.2
 */
@Data
public class TaskDetailDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 团队ID
     */
    private Long teamId;
    
    /**
     * 任务标题
     */
    private String title;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 任务状态 (TODO, DOING, REVIEW, DONE)
     */
    private String status;
    
    /**
     * 任务优先级 (LOW, MEDIUM, HIGH)
     */
    private String priority;
    
    /**
     * 截止日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;
    
    /**
     * 创建者用户ID
     */
    private Long createdBy;
    
    /**
     * 创建者名称
     */
    private String creatorName;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * 负责人列表
     */
    private List<TaskAssigneeDTO> assignees;
    
    /**
     * 评论列表
     */
    private List<TaskCommentDTO> comments;
    
    /**
     * 附件列表
     */
    private List<TaskAttachmentDTO> attachments;
    
    /**
     * 评论数量
     */
    private Integer commentCount;
    
    /**
     * 附件数量
     */
    private Integer attachmentCount;
}
