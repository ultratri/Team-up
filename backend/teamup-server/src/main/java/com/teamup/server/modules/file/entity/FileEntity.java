package com.teamup.server.modules.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件实体类
 * 对应数据库表: files
 */
@Data
@TableName("files")
public class FileEntity {
    
    /**
     * 文件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 团队ID
     */
    private Long teamId;
    
    /**
     * 上传者ID
     */
    private Long uploaderId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 文件夹路径
     */
    private String folderPath;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 父文件ID（用于版本控制）
     */
    private Long parentFileId;
    
    /**
     * 是否为文件夹
     */
    private Boolean isFolder;
    
    /**
     * 父文件夹ID
     */
    private Long parentFolderId;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadedAt;
}
