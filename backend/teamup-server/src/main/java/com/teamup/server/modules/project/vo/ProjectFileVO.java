package com.teamup.server.modules.project.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目文件VO
 */
@Data
public class ProjectFileVO {
    private Long id;
    private Long projectId;
    private Long uploaderId;
    private String fileName;

    // 对齐当前数据库结构：project_files.file_path
    private String filePath;

    private String fileType;
    private Long fileSize;

    // 对齐当前数据库结构：project_files.uploaded_at
    private LocalDateTime uploadedAt;

    // 上传者信息
    private String uploaderName;
    private String uploaderAvatar;
}
