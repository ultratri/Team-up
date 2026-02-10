package com.teamup.server.modules.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务附件DTO
 * Requirements: 2.1, 10.1, 10.2
 */
@Data
public class TaskAttachmentDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    
    /**
     * 上传者用户ID
     */
    private Long uploadedBy;
    
    /**
     * 上传者名称
     */
    private String uploaderName;
    
    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;
}
