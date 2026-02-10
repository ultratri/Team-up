package com.teamup.server.modules.file.service.impl;

import com.teamup.server.modules.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload.path:/var/uploads}")
    private String uploadPath;

    @Value("${file.access.url:http://localhost:8080/uploads}")
    private String accessUrl;

    private static final long MB = 1024 * 1024;
    private static final long MAX_FILE_SIZE = 100; // 100MB

    @PostConstruct
    public void initUploadDirs() {
        try {
            Path base = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(base.resolve("public"));
            Files.createDirectories(base.resolve("private"));
            log.info("文件上传目录初始化成功: base={}, public={}, private={}",
                    base, base.resolve("public"), base.resolve("private"));
        } catch (Exception e) {
            log.error("文件上传目录初始化失败: uploadPath={}", uploadPath, e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String category) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 团队文件共享不限制文件类型（由权限控制可见性/下载/预览）
        // 头像/封面等仍可按需限制类型，避免上传可执行内容伪装成图片
        if (!isTeamPrivateCategory(category)) {
        String[] allowedTypes = getAllowedTypes(category);
        if (!isValidFileType(file, allowedTypes)) {
            throw new RuntimeException("不支持的文件类型");
            }
        }

        // 验证文件大小
        long maxSize = getMaxSize(category);
        if (!isValidFileSize(file, maxSize)) {
            throw new RuntimeException("文件大小超过限制: " + maxSize + "MB");
        }

        try {
            // 生成文件路径: uploads/category/yyyy-MM-dd/uuid.ext
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = generateFileName(file.getOriginalFilename());
            String relativePath = buildRelativePath(category, date, fileName);
            
            // 创建目录
            Path dirPath = Paths.get(uploadPath).toAbsolutePath().normalize()
                    .resolve(getStorageRoot(category))
                    .resolve(category)
                    .resolve(date);
            Files.createDirectories(dirPath);
            
            // 保存文件
            Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());
            
            log.info("文件上传成功: {}", relativePath);
            
            return accessUrl + "/" + relativePath;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String saveFile(MultipartFile file, String directory) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 验证文件
        if (!validateFile(file)) {
            throw new RuntimeException("文件验证失败");
        }

        try {
            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename());
            
            // 创建目录
            Path dirPath = Paths.get(uploadPath, directory);
            Files.createDirectories(dirPath);
            
            // 保存文件
            Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());
            
            String relativePath = directory + "/" + fileName;
            log.info("文件保存成功: {}", relativePath);
            
            return relativePath;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        String relativePath = fileUrl;
        if (fileUrl.startsWith(accessUrl)) {
            relativePath = fileUrl.substring(accessUrl.length() + 1);
        }
        Path filePath = resolveStoragePath(relativePath);
        
        try {
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", relativePath);
        } catch (IOException e) {
            log.error("文件删除失败: {}", relativePath, e);
        }
    }

    @Override
    public Resource loadFile(String filePath) {
        try {
            String relativePath = filePath;
            if (filePath != null && filePath.startsWith(accessUrl)) {
                relativePath = filePath.substring(accessUrl.length() + 1);
            }
            Path file = resolveStoragePath(relativePath);
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("文件不存在或不可读: " + filePath);
            }
        } catch (MalformedURLException e) {
            log.error("文件加载失败: {}", filePath, e);
            throw new RuntimeException("文件加载失败: " + e.getMessage());
        }
    }

    private Path resolveStoragePath(String relativePath) {
        String rp = relativePath == null ? "" : relativePath;

        // 优先按当前规范解析：private/** 在 uploadPath/private；其它默认视为 public（uploadPath/public）
        if (rp.startsWith("private/") || rp.startsWith("public/")) {
            return Paths.get(uploadPath, rp);
        }

        // public URL/旧数据通常是 {category}/...，实际存储在 public/{category}/...
        return Paths.get(uploadPath, "public", rp);
    }

    @Override
    public boolean validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // 验证文件大小（10MB）
        if (!isValidFileSize(file, MAX_FILE_SIZE)) {
            return false;
        }

        // 验证文件类型
        String[] allowedTypes = new String[]{
            "image/jpeg", "image/jpg", "image/png", "image/gif",
            "application/pdf", 
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip", "application/x-rar-compressed"
        };
        
        return isValidFileType(file, allowedTypes);
    }

    @Override
    public boolean isValidFileType(MultipartFile file, String[] allowedTypes) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return Arrays.asList(allowedTypes).contains(contentType);
    }

    private boolean isTeamPrivateCategory(String category) {
        String c = category == null ? "" : category;
        return c.startsWith("team/");
    }

    private String getStorageRoot(String category) {
        return isTeamPrivateCategory(category) ? "private" : "public";
    }

    private String buildRelativePath(String category, String date, String fileName) {
        // public: URL 使用 /uploads/{category}/...（不带 public 前缀，匹配静态映射到 uploads/public）
        // private: 仅用于服务端鉴权下载/预览（不会被静态直链访问）
        if (isTeamPrivateCategory(category)) {
            return "private/" + category + "/" + date + "/" + fileName;
        }
        return category + "/" + date + "/" + fileName;
    }

    @Override
    public boolean isValidFileSize(MultipartFile file, long maxSizeInMB) {
        return file.getSize() <= maxSizeInMB * MB;
    }

    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private String[] getAllowedTypes(String category) {
        String normalizedCategory = category == null ? "" : category;
        if (normalizedCategory.startsWith("team/") || normalizedCategory.equals("team")) {
            return new String[]{
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/zip", "application/x-zip-compressed",
                "application/x-rar-compressed", "application/octet-stream",
                "text/plain", "application/json", "application/xml"
            };
        }

        return switch (normalizedCategory) {
            case "avatar", "cover" -> new String[]{
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
            };
            case "message" -> new String[]{
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/zip", "application/x-zip-compressed",
                "application/x-rar-compressed", "application/octet-stream",
                "text/plain", "application/json", "application/xml"
            };
            case "attachment" -> new String[]{
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            };
            default -> new String[]{"image/jpeg", "image/jpg", "image/png"};
        };
    }

    private long getMaxSize(String category) {
        String normalizedCategory = category == null ? "" : category;
        if (normalizedCategory.startsWith("team/") || normalizedCategory.equals("team")) {
            return 100; // 100MB
        }

        return switch (normalizedCategory) {
            case "avatar" -> 2;      // 2MB
            case "cover" -> 5;       // 5MB
            case "message" -> 100;     // 100MB
            case "attachment" -> 100; // 100MB
            default -> 2;
        };
    }
}
