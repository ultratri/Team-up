package com.teamup.server.modules.file.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件视图对象
 */
@Data
public class FileVO {
    /**
     * 文件ID
     */
    private Long id;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 是否为文件夹
     */
    private Boolean isFolder;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 上传者ID
     */
    private Long uploaderId;
    
    /**
     * 上传者名称
     */
    private String uploaderName;
    
    /**
     * 父文件夹ID
     */
    private Long parentFolderId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
