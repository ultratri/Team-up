package com.teamup.server.modules.file.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * 上传头像
     */
    @PostMapping("/upload/avatar")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadFile(file, "avatar");
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success(result);
    }

    /**
     * 上传封面
     */
    @PostMapping("/upload/cover")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadFile(file, "cover");
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success(result);
    }

    /**
     * 上传附件
     */
    @PostMapping("/upload/attachment")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadFile(file, "attachment");
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success(result);
    }

    /**
     * 上传消息图片
     */
    @PostMapping("/upload/message-image")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> uploadMessageImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadFile(file, "message");
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("fileName", file.getOriginalFilename());
        result.put("fileSize", file.getSize());
        return Result.success(result);
    }

    /**
     * 上传消息文件
     */
    @PostMapping("/upload/message-file")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> uploadMessageFile(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadFile(file, "message");
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("fileName", file.getOriginalFilename());
        result.put("fileSize", file.getSize());
        return Result.success(result);
    }

    /**
     * 删除文件
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteFile(@RequestParam String url) {
        fileStorageService.deleteFile(url);
        return Result.success();
    }
}
