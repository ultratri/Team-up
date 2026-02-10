package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.team.dto.TaskAttachmentDTO;
import com.teamup.server.modules.team.entity.TaskAttachment;
import com.teamup.server.modules.team.mapper.TaskAttachmentMapper;
import com.teamup.server.modules.team.service.TaskAttachmentService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务附件服务实现
 * Requirements: 4.1, 4.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAttachmentServiceImpl extends ServiceImpl<TaskAttachmentMapper, TaskAttachment> implements TaskAttachmentService {

    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;

    @Value("${file.access.url:http://localhost:8080/uploads}")
    private String accessUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskAttachmentDTO uploadAttachment(Long taskId, MultipartFile file, Long uploadedBy) {
        // Validate file
        if (!fileStorageService.validateFile(file)) {
            throw new RuntimeException("文件验证失败：文件大小超过10MB或文件类型不支持");
        }
        
        // Save file to storage
        String directory = "tasks/" + taskId;
        String filePath = fileStorageService.saveFile(file, directory);
        
        // Create attachment record
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setUploadedBy(uploadedBy);
        attachment.setUploadedAt(LocalDateTime.now());
        
        save(attachment);
        
        log.info("任务附件上传成功: taskId={}, fileName={}", taskId, file.getOriginalFilename());
        
        return convertToDTO(attachment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long attachmentId) {
        TaskAttachment attachment = getById(attachmentId);
        if (attachment == null) {
            throw new RuntimeException("附件不存在");
        }
        
        // Delete file from storage
        String fileUrl = accessUrl + "/" + attachment.getFilePath();
        fileStorageService.deleteFile(fileUrl);
        
        // Delete database record
        removeById(attachmentId);
        
        log.info("任务附件删除成功: attachmentId={}, fileName={}", attachmentId, attachment.getFileName());
    }

    @Override
    public List<TaskAttachmentDTO> getAttachmentsByTaskId(Long taskId) {
        LambdaQueryWrapper<TaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskAttachment::getTaskId, taskId)
                   .orderByDesc(TaskAttachment::getUploadedAt);
        
        List<TaskAttachment> attachments = list(queryWrapper);
        
        // Batch query optimization: collect all user IDs first
        List<Long> userIds = attachments.stream()
                .map(TaskAttachment::getUploadedBy)
                .distinct()
                .toList();
        
        if (userIds.isEmpty()) {
            return List.of();
        }
        
        // Batch query users
        List<User> users = userMapper.selectBatchIds(userIds);
        
        // Create map for O(1) lookup
        var userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        
        // Convert to DTOs using cached data
        List<TaskAttachmentDTO> dtoList = new ArrayList<>();
        for (TaskAttachment attachment : attachments) {
            dtoList.add(convertToDTOWithCache(attachment, userMap));
        }
        
        return dtoList;
    }

    @Override
    public Resource downloadAttachment(Long attachmentId) {
        TaskAttachment attachment = getById(attachmentId);
        if (attachment == null) {
            throw new RuntimeException("附件不存在");
        }
        
        return fileStorageService.loadFile(attachment.getFilePath());
    }
    
    /**
     * 转换实体为DTO
     */
    private TaskAttachmentDTO convertToDTO(TaskAttachment attachment) {
        TaskAttachmentDTO dto = new TaskAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setTaskId(attachment.getTaskId());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setUploadedBy(attachment.getUploadedBy());
        dto.setUploadedAt(attachment.getUploadedAt());
        
        // Get uploader name
        User user = userMapper.selectById(attachment.getUploadedBy());
        if (user != null) {
            dto.setUploaderName(user.getUsername());
        }
        
        return dto;
    }
    
    /**
     * 转换实体为DTO（使用缓存数据，避免N+1查询）
     */
    private TaskAttachmentDTO convertToDTOWithCache(
            TaskAttachment attachment,
            java.util.Map<Long, User> userMap) {
        TaskAttachmentDTO dto = new TaskAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setTaskId(attachment.getTaskId());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setUploadedBy(attachment.getUploadedBy());
        dto.setUploadedAt(attachment.getUploadedAt());
        
        // Get uploader name from cache
        User user = userMap.get(attachment.getUploadedBy());
        if (user != null) {
            dto.setUploaderName(user.getUsername());
        }
        
        return dto;
    }
}
