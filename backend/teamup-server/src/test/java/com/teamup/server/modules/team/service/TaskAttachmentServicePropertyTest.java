package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.team.dto.TaskAttachmentDTO;
import com.teamup.server.modules.team.entity.TaskAttachment;
import com.teamup.server.modules.team.mapper.TaskAttachmentMapper;
import com.teamup.server.modules.team.service.impl.TaskAttachmentServiceImpl;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 任务附件服务属性测试
 * Feature: task-board-enhancement
 * 
 * 使用模拟对象测试服务层逻辑
 */
public class TaskAttachmentServicePropertyTest {

    /**
     * Property 11: 附件上传和存储
     * For any valid file upload to a task, the system should store both the file in the 
     * file system and create a corresponding record in task_attachments table.
     * Validates: Requirements 4.1
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 11: 附件上传和存储")
    void attachmentUploadShouldStoreFileAndRecord(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int userIdSeed,
            @ForAll @StringLength(min = 5, max = 50) String fileName) {
        
        Long taskId = (long) taskIdSeed;
        Long userId = (long) userIdSeed;
        
        // Setup mocks
        TaskAttachmentMapper taskAttachmentMapper = mock(TaskAttachmentMapper.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        UserMapper userMapper = mock(UserMapper.class);
        TaskAttachmentServiceImpl taskAttachmentService = new TaskAttachmentServiceImpl(fileStorageService, userMapper);
        
        // Inject the mocked mapper
        ReflectionTestUtils.setField(taskAttachmentService, "baseMapper", taskAttachmentMapper);
        ReflectionTestUtils.setField(taskAttachmentService, "accessUrl", "http://localhost:8080/uploads");
        
        // Mock user data
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        
        // Track attachments and files in memory
        List<TaskAttachment> attachmentList = new ArrayList<>();
        Map<String, byte[]> fileStorage = new HashMap<>();
        
        // Create a valid test file
        byte[] fileContent = "test file content".getBytes();
        MultipartFile file = new MockMultipartFile(
            "file",
            fileName + ".pdf",
            "application/pdf",
            fileContent
        );
        
        // Mock file validation (valid file)
        when(fileStorageService.validateFile(any(MultipartFile.class))).thenReturn(true);
        
        // Mock file save
        when(fileStorageService.saveFile(any(MultipartFile.class), anyString())).thenAnswer(invocation -> {
            MultipartFile f = invocation.getArgument(0);
            String directory = invocation.getArgument(1);
            String filePath = directory + "/" + f.getOriginalFilename();
            fileStorage.put(filePath, fileContent);
            return filePath;
        });
        
        // Mock insert behavior
        when(taskAttachmentMapper.insert(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId((long) (attachmentList.size() + 1));
            attachmentList.add(attachment);
            return 1;
        });
        
        // Execute: Upload attachment
        TaskAttachmentDTO uploadedAttachment = taskAttachmentService.uploadAttachment(taskId, file, userId);
        
        // Verify: Attachment record should be created
        assertNotNull(uploadedAttachment, "Uploaded attachment should not be null");
        assertEquals(taskId, uploadedAttachment.getTaskId(), "Task ID should match");
        assertEquals(userId, uploadedAttachment.getUploadedBy(), "Uploader ID should match");
        assertNotNull(uploadedAttachment.getFileName(), "File name should be set");
        assertNotNull(uploadedAttachment.getUploadedAt(), "Upload time should be set");
        
        // Verify: File should be stored
        verify(fileStorageService, times(1)).saveFile(any(MultipartFile.class), anyString());
        assertFalse(fileStorage.isEmpty(), "File should be stored in file system");
        
        // Verify: Database record should be created
        assertEquals(1, attachmentList.size(), "One attachment record should be created");
        TaskAttachment storedAttachment = attachmentList.get(0);
        assertEquals(taskId, storedAttachment.getTaskId(), "Stored task ID should match");
        assertEquals(userId, storedAttachment.getUploadedBy(), "Stored uploader ID should match");
    }

    /**
     * Property 12: 附件上传下载往返一致性
     * For any file uploaded as an attachment, downloading the attachment should return 
     * a file with identical content to the original upload.
     * Validates: Requirements 4.2
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 12: 附件上传下载往返一致性")
    void attachmentUploadDownloadRoundTripConsistency(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int userIdSeed,
            @ForAll @StringLength(min = 10, max = 100) String fileContent) {
        
        Long taskId = (long) taskIdSeed;
        Long userId = (long) userIdSeed;
        
        // Setup mocks
        TaskAttachmentMapper taskAttachmentMapper = mock(TaskAttachmentMapper.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        UserMapper userMapper = mock(UserMapper.class);
        TaskAttachmentServiceImpl taskAttachmentService = new TaskAttachmentServiceImpl(fileStorageService, userMapper);
        
        // Inject the mocked mapper
        ReflectionTestUtils.setField(taskAttachmentService, "baseMapper", taskAttachmentMapper);
        ReflectionTestUtils.setField(taskAttachmentService, "accessUrl", "http://localhost:8080/uploads");
        
        // Mock user data
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        
        // Track attachments and files
        List<TaskAttachment> attachmentList = new ArrayList<>();
        Map<String, byte[]> fileStorage = new HashMap<>();
        
        // Create test file
        byte[] originalContent = fileContent.getBytes();
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            originalContent
        );
        
        // Mock file validation
        when(fileStorageService.validateFile(any(MultipartFile.class))).thenReturn(true);
        
        // Mock file save
        when(fileStorageService.saveFile(any(MultipartFile.class), anyString())).thenAnswer(invocation -> {
            MultipartFile f = invocation.getArgument(0);
            String directory = invocation.getArgument(1);
            String filePath = directory + "/" + f.getOriginalFilename();
            fileStorage.put(filePath, originalContent);
            return filePath;
        });
        
        // Mock insert
        when(taskAttachmentMapper.insert(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId((long) (attachmentList.size() + 1));
            attachmentList.add(attachment);
            return 1;
        });
        
        // Mock selectById for download
        when(taskAttachmentMapper.selectById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return attachmentList.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
        });
        
        // Mock file load
        when(fileStorageService.loadFile(anyString())).thenAnswer(invocation -> {
            String filePath = invocation.getArgument(0);
            byte[] content = fileStorage.get(filePath);
            if (content != null) {
                return new ByteArrayResource(content);
            }
            throw new RuntimeException("File not found");
        });
        
        // Execute: Upload attachment
        TaskAttachmentDTO uploadedAttachment = taskAttachmentService.uploadAttachment(taskId, file, userId);
        Long attachmentId = uploadedAttachment.getId();
        
        // Execute: Download attachment
        Resource downloadedResource = taskAttachmentService.downloadAttachment(attachmentId);
        
        // Verify: Downloaded content should match original
        assertNotNull(downloadedResource, "Downloaded resource should not be null");
        try {
            byte[] downloadedContent = downloadedResource.getInputStream().readAllBytes();
            assertArrayEquals(originalContent, downloadedContent,
                "Downloaded file content should match original upload content");
        } catch (Exception e) {
            fail("Failed to read downloaded content: " + e.getMessage());
        }
    }

    /**
     * Property 13: 附件删除完整性
     * For any attachment, when deleted, both the file in storage and the database 
     * record should be removed.
     * Validates: Requirements 4.3
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 13: 附件删除完整性")
    void attachmentDeletionShouldRemoveFileAndRecord(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int userIdSeed) {
        
        Long taskId = (long) taskIdSeed;
        Long userId = (long) userIdSeed;
        
        // Setup mocks
        TaskAttachmentMapper taskAttachmentMapper = mock(TaskAttachmentMapper.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        UserMapper userMapper = mock(UserMapper.class);
        TaskAttachmentServiceImpl taskAttachmentService = spy(new TaskAttachmentServiceImpl(fileStorageService, userMapper));
        
        // Inject the mocked mapper
        ReflectionTestUtils.setField(taskAttachmentService, "baseMapper", taskAttachmentMapper);
        ReflectionTestUtils.setField(taskAttachmentService, "accessUrl", "http://localhost:8080/uploads");
        
        // Mock user data
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        
        // Track attachments and files
        List<TaskAttachment> attachmentList = new ArrayList<>();
        Map<String, byte[]> fileStorage = new HashMap<>();
        
        // Create test file
        byte[] fileContent = "test content".getBytes();
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            fileContent
        );
        
        // Mock file validation
        when(fileStorageService.validateFile(any(MultipartFile.class))).thenReturn(true);
        
        // Mock file save
        when(fileStorageService.saveFile(any(MultipartFile.class), anyString())).thenAnswer(invocation -> {
            String filePath = "tasks/" + taskId + "/test.pdf";
            fileStorage.put(filePath, fileContent);
            return filePath;
        });
        
        // Mock insert
        when(taskAttachmentMapper.insert(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId((long) (attachmentList.size() + 1));
            attachmentList.add(attachment);
            return 1;
        });
        
        // Mock selectById
        when(taskAttachmentMapper.selectById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return attachmentList.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
        });
        
        // Mock removeById (used by ServiceImpl)
        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            int sizeBefore = attachmentList.size();
            attachmentList.removeIf(a -> a.getId().equals(id));
            return sizeBefore > attachmentList.size();
        }).when(taskAttachmentService).removeById(any(Long.class));
        
        // Mock file delete
        doAnswer(invocation -> {
            String fileUrl = invocation.getArgument(0);
            String filePath = fileUrl.replace("http://localhost:8080/uploads/", "");
            fileStorage.remove(filePath);
            return null;
        }).when(fileStorageService).deleteFile(anyString());
        
        // Execute: Upload attachment
        TaskAttachmentDTO uploadedAttachment = taskAttachmentService.uploadAttachment(taskId, file, userId);
        Long attachmentId = uploadedAttachment.getId();
        
        // Verify: Attachment and file exist
        assertEquals(1, attachmentList.size(), "Attachment record should exist");
        assertEquals(1, fileStorage.size(), "File should exist in storage");
        
        // Execute: Delete attachment
        taskAttachmentService.deleteAttachment(attachmentId);
        
        // Verify: Both file and record should be removed
        assertEquals(0, attachmentList.size(), "Attachment record should be removed from database");
        assertEquals(0, fileStorage.size(), "File should be removed from storage");
        verify(fileStorageService, times(1)).deleteFile(anyString());
    }
}
