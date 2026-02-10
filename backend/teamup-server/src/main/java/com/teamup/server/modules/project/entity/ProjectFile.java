package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目文件实体
 */
@Data
@TableName("project_files")
public class ProjectFile {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long uploaderId;

    private String fileName;

    // 对齐当前数据库结构：project_files.file_path
    private String filePath;

    private Long fileSize;
    private String fileType;

    // 对齐当前数据库结构：project_files.uploaded_at
    private LocalDateTime uploadedAt;
}
