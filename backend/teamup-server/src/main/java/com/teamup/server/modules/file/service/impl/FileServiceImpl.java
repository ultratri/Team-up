package com.teamup.server.modules.file.service.impl;

import com.teamup.server.common.audit.AuditLogService;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.common.security.PathValidator;
import com.teamup.server.common.security.PermissionChecker;
import com.teamup.server.modules.activity.service.ActivityService;
import com.teamup.server.modules.file.entity.FileEntity;
import com.teamup.server.modules.file.mapper.FileMapper;
import com.teamup.server.modules.file.service.FileService;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.file.vo.FileVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    
    private final FileMapper fileMapper;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final ActivityService activityService;
    private final PermissionChecker permissionChecker;
    private final PathValidator pathValidator;
    private final AuditLogService auditLogService;
    
    @Value("${file.upload.path:/var/uploads}")
    private String uploadPath;
    
    @Value("${file.access.url:http://localhost:8080/uploads}")
    private String accessUrl;
    
    @Value("${file.storage.type:local}")
    private String storageType;
    
    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;
    
    @Value("${minio.bucket:teamup}")
    private String minioBucket;
    
    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;
    
    @Value("${minio.secret-key:minioadmin}")
    private String minioSecretKey;
    
    @Override
    public List<FileVO> getFileList(Long teamId, Long folderId) {
        // 查询文件列表（按创建时间倒序排序）
        List<FileEntity> fileEntities = fileMapper.selectByTeamAndFolder(teamId, folderId);
        
        // 获取所有上传者ID
        List<Long> uploaderIds = fileEntities.stream()
                .map(FileEntity::getUploaderId)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询用户信息
        Map<Long, String> uploaderNameMap = uploaderIds.isEmpty() ? 
                Map.of() : 
                userMapper.selectBatchIds(uploaderIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));
        
        // 为文件夹计算聚合大小（包含直接子文件的总大小）
        for (FileEntity entity : fileEntities) {
            if (Boolean.TRUE.equals(entity.getIsFolder())) {
                Long folderSize = fileMapper.sumFileSizeByFolder(teamId, entity.getId());
                entity.setFileSize(folderSize != null ? folderSize : 0L);
            }
        }

        // 转换为VO
        return fileEntities.stream()
                .map(entity -> convertToVO(entity, uploaderNameMap))
                .collect(Collectors.toList());
    }
    
    @Override
    public void downloadFile(Long fileId, HttpServletResponse response) {
        // 查询文件信息
        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在");
        }
        
        // 检查是否为文件夹
        if (Boolean.TRUE.equals(fileEntity.getIsFolder())) {
            throw new BusinessException("不能下载文件夹");
        }
        
        // 根据存储类型处理下载
        if ("minio".equalsIgnoreCase(storageType)) {
            // MinIO存储：生成预签名URL并重定向
            String presignedUrl = generatePresignedUrl(fileId, 60);
            try {
                response.sendRedirect(presignedUrl);
            } catch (IOException e) {
                log.error("重定向到预签名URL失败", e);
                throw new BusinessException("文件下载失败");
            }
        } else {
            // 本地存储：流式传输
            streamLocalFile(fileEntity, response);
        }
    }

    @Override
    public void previewFile(Long fileId, HttpServletResponse response) {
        // 查询文件信息
        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在");
        }

        // 检查是否为文件夹
        if (Boolean.TRUE.equals(fileEntity.getIsFolder())) {
            throw new BusinessException("不能预览文件夹");
        }

        // 根据存储类型处理预览
        if ("minio".equalsIgnoreCase(storageType)) {
            // 简化：MinIO 仍使用预签名URL（后续可改为服务端代理以避免URL外泄）
            String presignedUrl = generatePresignedUrl(fileId, 10);
            try {
                response.sendRedirect(presignedUrl);
            } catch (IOException e) {
                log.error("重定向到预签名URL失败", e);
                throw new BusinessException("文件预览失败");
            }
        } else {
            // 本地存储：inline 流式传输
            streamLocalFile(fileEntity, response, true);
        }
    }
    
    @Override
    public String generatePresignedUrl(Long fileId, int expirationMinutes) {
        // 查询文件信息
        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在");
        }
        
        if (!"minio".equalsIgnoreCase(storageType)) {
            throw new BusinessException("当前存储类型不支持预签名URL");
        }
        
        // 生成MinIO预签名URL
        // 注意：这里简化实现，实际应该使用MinIO SDK
        // 格式: http://minio-endpoint/bucket/file-path?X-Amz-Expires=expiration
        long expirationSeconds = expirationMinutes * 60L;
        long expirationTimestamp = LocalDateTime.now().plusMinutes(expirationMinutes)
                .toEpochSecond(ZoneOffset.UTC);
        
        String filePath = fileEntity.getFilePath();
        if (filePath.startsWith(accessUrl)) {
            filePath = filePath.substring(accessUrl.length() + 1);
        }
        
        // 简化的预签名URL（实际应该包含签名）
        String presignedUrl = String.format("%s/%s/%s?X-Amz-Expires=%d&X-Amz-Date=%d",
                minioEndpoint, minioBucket, filePath, expirationSeconds, expirationTimestamp);
        
        log.info("生成预签名URL: fileId={}, url={}", fileId, presignedUrl);
        return presignedUrl;
    }
    
    /**
     * 流式传输本地文件
     */
    private void streamLocalFile(FileEntity fileEntity, HttpServletResponse response) {
        streamLocalFile(fileEntity, response, false);
    }

    private void streamLocalFile(FileEntity fileEntity, HttpServletResponse response, boolean inline) {
        String filePath = fileEntity.getFilePath();
        
        // 如果filePath是完整URL，提取相对路径
        if (filePath.startsWith(accessUrl)) {
            filePath = filePath.substring(accessUrl.length() + 1);
        }

        // 验证路径安全性
        pathValidator.validateFilePath(filePath);

        // 兼容 public/private 存储根：
        // - private/** 直接在 uploadPath/private
        // - 其它（avatar/cover/message 等）默认在 uploadPath/public 下
        Path file = resolveLocalFilePath(filePath);

        if (!Files.exists(file)) {
            throw new BusinessException("文件不存在");
        }
        
        try {
            // 设置响应头
            String fileName = fileEntity.getFileName();
            
            // 验证文件名安全性
            pathValidator.validateFileName(fileName);
            
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            String mime = fileEntity.getMimeType() != null ? fileEntity.getMimeType() : "application/octet-stream";
            if (mime.startsWith("text/") || "application/json".equalsIgnoreCase(mime) || "application/xml".equalsIgnoreCase(mime)) {
                response.setContentType(mime + ";charset=UTF-8");
            } else {
                response.setContentType(mime);
            }
            response.setHeader("Content-Disposition", 
                    (inline ? "inline" : "attachment") + "; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(fileEntity.getFileSize() != null ? 
                    fileEntity.getFileSize() : Files.size(file));
            
            // 流式传输文件
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toFile()));
                 OutputStream os = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            
            if (inline) {
                log.info("文件预览成功: fileId={}, fileName={}", fileEntity.getId(), fileName);
            } else {
                log.info("文件下载成功: fileId={}, fileName={}", fileEntity.getId(), fileName);
            }
            
        } catch (IOException e) {
            log.error("文件下载失败: fileId={}", fileEntity.getId(), e);
            throw new BusinessException("文件下载失败");
        }
    }

    private Path resolveLocalFilePath(String relativePath) {
        // 先尝试原路径（兼容旧数据：uploadPath/{category}/...）
        Path direct = pathValidator.safeJoinPath(uploadPath, relativePath);
        if (Files.exists(direct)) return direct;

        // 尝试 public 根（当前静态资源映射）
        if (!relativePath.startsWith("public/") && !relativePath.startsWith("private/")) {
            Path inPublic = pathValidator.safeJoinPath(uploadPath, "public/" + relativePath);
            if (Files.exists(inPublic)) return inPublic;
        }

        // 最后尝试 private 根（避免历史 team/ 数据仍能读取）
        if (!relativePath.startsWith("private/")) {
            Path inPrivate = pathValidator.safeJoinPath(uploadPath, "private/" + relativePath);
            if (Files.exists(inPrivate)) return inPrivate;
        }

        return direct;
    }
    
    /**
     * 将FileEntity转换为FileVO
     */
    private FileVO convertToVO(FileEntity entity, Map<Long, String> uploaderNameMap) {
        FileVO vo = new FileVO();
        vo.setId(entity.getId());
        vo.setFileName(entity.getFileName());
        vo.setIsFolder(entity.getIsFolder() != null ? entity.getIsFolder() : false);
        vo.setFileSize(entity.getFileSize());
        vo.setMimeType(entity.getMimeType());
        vo.setFileType(entity.getFileType());
        vo.setUploaderId(entity.getUploaderId());
        vo.setUploaderName(uploaderNameMap.getOrDefault(entity.getUploaderId(), "Unknown"));
        vo.setCreatedAt(entity.getUploadedAt());
        return vo;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId, Long userId) {
        // 1. 查询文件信息
        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        
        // 2. 检查是否为文件夹
        if (Boolean.TRUE.equals(file.getIsFolder())) {
            throw new BusinessException("请使用删除文件夹接口");
        }
        
        // 3. 验证权限（只有上传者或团队管理者可以删除）
        permissionChecker.requireFileDeletePermission(file, userId);
        
        try {
            // 4. 删除数据库记录
            fileMapper.deleteById(fileId);
            
            // 5. 删除存储文件
            if (file.getFilePath() != null && !file.getFilePath().isEmpty()) {
                fileStorageService.deleteFile(file.getFilePath());
            }
            
            // 6. 记录活动
            activityService.trackFileActivity(
                file.getTeamId(),
                userId,
                "delete",
                "删除了文件「" + file.getFileName() + "」",
                fileId
            );
            
            // 7. 记录审计日志
            auditLogService.logFileDelete(fileId, file.getFileName(), "SUCCESS", null);
            
            log.info("文件删除成功: fileId={}, fileName={}, userId={}", 
                    fileId, file.getFileName(), userId);
            
        } catch (Exception e) {
            // 记录失败的审计日志
            auditLogService.logFileDelete(fileId, file.getFileName(), "FAILURE", e.getMessage());
            
            log.error("文件删除失败: fileId={}", fileId, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long folderId, Long userId) {
        // 1. 查询文件夹信息
        FileEntity folder = fileMapper.selectById(folderId);
        if (folder == null) {
            throw new BusinessException("文件夹不存在");
        }
        
        // 2. 检查是否为文件夹
        if (!Boolean.TRUE.equals(folder.getIsFolder())) {
            throw new BusinessException("该对象不是文件夹");
        }
        
        // 3. 验证权限
        permissionChecker.requireFileDeletePermission(folder, userId);
        
        try {
            // 4. 递归删除所有子文件和子文件夹
            deleteChildrenRecursively(folderId, userId, folder.getTeamId());
            
            // 5. 删除文件夹本身
            fileMapper.deleteById(folderId);
            
            // 6. 记录活动
            activityService.trackFileActivity(
                folder.getTeamId(),
                userId,
                "delete",
                "删除了文件夹「" + folder.getFileName() + "」",
                folderId
            );
            
            // 7. 记录审计日志
            auditLogService.logFolderDelete(folderId, folder.getFileName(), "SUCCESS", null);
            
            log.info("文件夹删除成功: folderId={}, folderName={}, userId={}", 
                    folderId, folder.getFileName(), userId);
            
        } catch (Exception e) {
            // 记录失败的审计日志
            auditLogService.logFolderDelete(folderId, folder.getFileName(), "FAILURE", e.getMessage());
            
            log.error("文件夹删除失败: folderId={}", folderId, e);
            throw new BusinessException("文件夹删除失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO uploadTeamFile(MultipartFile file, Long teamId, Long folderId, Long userId) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        
        // 上传文件到存储
        String storedPath = fileStorageService.uploadFile(file, "team/" + teamId);
        
        // 创建文件记录
        FileEntity fileEntity = new FileEntity();
        fileEntity.setTeamId(teamId);
        fileEntity.setUploaderId(userId);
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFilePath(storedPath);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(getFileExtension(file.getOriginalFilename()));
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setIsFolder(false);
        fileEntity.setParentFolderId(folderId);
        fileEntity.setUploadedAt(LocalDateTime.now());
        fileEntity.setVersion(1);
        
        fileMapper.insert(fileEntity);
        
        // 获取上传者信息
        User uploader = userMapper.selectById(userId);
        String uploaderName = uploader != null ? uploader.getUsername() : "未知用户";
        
        return convertToVO(fileEntity, uploaderName);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO createFolder(Long teamId, String folderName, Long parentFolderId, Long userId) {
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new BusinessException("文件夹名称不能为空");
        }
        
        // 创建文件夹记录
        FileEntity folder = new FileEntity();
        folder.setTeamId(teamId);
        folder.setUploaderId(userId);
        folder.setFileName(folderName.trim());
        folder.setFilePath("");
        folder.setFileSize(0L);
        folder.setIsFolder(true);
        folder.setParentFolderId(parentFolderId);
        folder.setUploadedAt(LocalDateTime.now());
        
        fileMapper.insert(folder);
        
        // 获取创建者信息
        User creator = userMapper.selectById(userId);
        String creatorName = creator != null ? creator.getUsername() : "未知用户";
        
        return convertToVO(folder, creatorName);
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private FileVO convertToVO(FileEntity entity, String uploaderName) {
        FileVO vo = new FileVO();
        vo.setId(entity.getId());
        vo.setFileName(entity.getFileName());
        vo.setFileSize(entity.getFileSize());
        vo.setFileType(entity.getFileType());
        vo.setMimeType(entity.getMimeType());
        vo.setUploaderId(entity.getUploaderId());
        vo.setUploaderName(uploaderName);
        vo.setIsFolder(entity.getIsFolder());
        vo.setParentFolderId(entity.getParentFolderId());
        vo.setCreatedAt(entity.getUploadedAt() != null ? entity.getUploadedAt() : LocalDateTime.now());
        return vo;
    }
    
    /**
     * 递归删除文件夹内的所有子文件和子文件夹
     */
    private void deleteChildrenRecursively(Long folderId, Long userId, Long teamId) {
        // 查询文件夹内的所有文件和子文件夹
        List<FileEntity> children = fileMapper.selectByTeamAndFolder(teamId, folderId);
        
        for (FileEntity child : children) {
            if (Boolean.TRUE.equals(child.getIsFolder())) {
                // 递归删除子文件夹
                deleteChildrenRecursively(child.getId(), userId, teamId);
                fileMapper.deleteById(child.getId());
            } else {
                // 删除文件
                fileMapper.deleteById(child.getId());
                if (child.getFilePath() != null && !child.getFilePath().isEmpty()) {
                    try {
                        fileStorageService.deleteFile(child.getFilePath());
                    } catch (Exception e) {
                        log.warn("删除子文件失败: fileId={}, filePath={}", 
                                child.getId(), child.getFilePath(), e);
                    }
                }
            }
        }
    }
}
