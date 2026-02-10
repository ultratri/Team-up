package com.teamup.server.modules.file.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {
    /**
     * 上传文件
     * @param file 文件
     * @param category 分类（avatar, cover, attachment）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String category);
    
    /**
     * 保存文件到指定目录
     * @param file 文件
     * @param directory 目录
     * @return 文件相对路径
     */
    String saveFile(MultipartFile file, String directory);
    
    /**
     * 删除文件
     */
    void deleteFile(String fileUrl);
    
    /**
     * 加载文件
     * @param filePath 文件路径
     * @return 文件资源
     */
    Resource loadFile(String filePath);
    
    /**
     * 验证文件（大小和类型）
     * @param file 文件
     * @return 是否有效
     */
    boolean validateFile(MultipartFile file);
    
    /**
     * 验证文件类型
     */
    boolean isValidFileType(MultipartFile file, String[] allowedTypes);
    
    /**
     * 验证文件大小
     */
    boolean isValidFileSize(MultipartFile file, long maxSizeInMB);
}
