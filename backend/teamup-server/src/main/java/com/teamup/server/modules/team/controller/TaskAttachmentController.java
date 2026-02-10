package com.teamup.server.modules.team.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.team.dto.TaskAttachmentDTO;
import com.teamup.server.modules.team.service.TaskAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 任务附件控制器
 * Requirements: 4.1, 4.2, 4.3, 4.5
 */
@RestController
@RequestMapping("/tasks/{taskId}/attachments")
@RequiredArgsConstructor
public class TaskAttachmentController {

    private final TaskAttachmentService taskAttachmentService;

    /**
     * 上传任务附件
     * POST /tasks/{taskId}/attachments
     * 
     * @param taskId 任务ID
     * @param file 文件
     * @param uploadedBy 上传者ID（从请求参数获取）
     * @return 附件DTO
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TaskAttachmentDTO> uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") Long uploadedBy) {
        
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        
        try {
            TaskAttachmentDTO attachment = taskAttachmentService.uploadAttachment(taskId, file, uploadedBy);
            return Result.success(attachment);
        } catch (Exception e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务附件
     * DELETE /tasks/{taskId}/attachments/{attachmentId}
     * 
     * @param taskId 任务ID
     * @param attachmentId 附件ID
     * @return 成功响应
     */
    @DeleteMapping("/{attachmentId}")
    public Result<Void> deleteAttachment(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId) {
        
        try {
            taskAttachmentService.deleteAttachment(attachmentId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("附件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务的所有附件
     * GET /tasks/{taskId}/attachments
     * 
     * @param taskId 任务ID
     * @return 附件DTO列表
     */
    @GetMapping
    public Result<List<TaskAttachmentDTO>> getAttachments(@PathVariable Long taskId) {
        try {
            List<TaskAttachmentDTO> attachments = taskAttachmentService.getAttachmentsByTaskId(taskId);
            return Result.success(attachments);
        } catch (Exception e) {
            return Result.error("获取附件列表失败: " + e.getMessage());
        }
    }

    /**
     * 下载任务附件
     * GET /tasks/{taskId}/attachments/{attachmentId}/download
     * 
     * @param taskId 任务ID
     * @param attachmentId 附件ID
     * @return 文件资源
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId) {
        
        try {
            Resource resource = taskAttachmentService.downloadAttachment(attachmentId);
            
            // Get attachment info for filename
            TaskAttachmentDTO attachment = taskAttachmentService.getAttachmentsByTaskId(taskId)
                    .stream()
                    .filter(a -> a.getId().equals(attachmentId))
                    .findFirst()
                    .orElse(null);
            
            String fileName = attachment != null ? attachment.getFileName() : "download";
            
            // Encode filename for Content-Disposition header
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + encodedFileName + "\"")
                    .body(resource);
        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
