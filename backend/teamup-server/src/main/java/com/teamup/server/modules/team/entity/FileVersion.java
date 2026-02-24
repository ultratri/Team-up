package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件版本历史实体类
 */
@Data
@TableName("file_versions")
public class FileVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long fileId;
    private Integer versionNumber;
    private String filePath;
    private Long fileSize;
    private Long uploadedBy;
    private String changeDescription;
    private LocalDateTime uploadedAt;
}
