package com.teamup.server.modules.file.service;

import com.teamup.server.modules.file.vo.FileVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {
    
    /**
     * 获取团队文件列表
     * 
     * @param teamId 团队ID
     * @param folderId 文件夹ID（null表示根目录）
     * @return 文件列表
     */
    List<FileVO> getFileList(Long teamId, Long folderId);
    
    /**
     * 下载文件
     * 
     * @param fileId 文件ID
     * @param response HTTP响应对象
     */
    void downloadFile(Long fileId, HttpServletResponse response);

    /**
     * 预览文件（inline），用于图片/PDF/文本等在浏览器中直接查看
     */
    void previewFile(Long fileId, HttpServletResponse response);
    
    /**
     * 生成预签名URL（用于云端文件）
     * 
     * @param fileId 文件ID
     * @param expirationMinutes 过期时间（分钟）
     * @return 预签名URL
     */
    String generatePresignedUrl(Long fileId, int expirationMinutes);
    
    /**
     * 删除文件
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    void deleteFile(Long fileId, Long userId);
    
    /**
     * 删除文件夹（递归删除所有子文件和子文件夹）
     * 
     * @param folderId 文件夹ID
     * @param userId 用户ID
     */
    void deleteFolder(Long folderId, Long userId);
    
    /**
     * 上传团队文件
     * 
     * @param file 文件
     * @param teamId 团队ID
     * @param folderId 文件夹ID（可选）
     * @param userId 上传者ID
     * @return 文件信息
     */
    FileVO uploadTeamFile(MultipartFile file, Long teamId, Long folderId, Long userId);
    
    /**
     * 创建文件夹
     * 
     * @param teamId 团队ID
     * @param folderName 文件夹名称
     * @param parentFolderId 父文件夹ID（可选）
     * @param userId 创建者ID
     * @return 文件夹信息
     */
    FileVO createFolder(Long teamId, String folderName, Long parentFolderId, Long userId);
}
