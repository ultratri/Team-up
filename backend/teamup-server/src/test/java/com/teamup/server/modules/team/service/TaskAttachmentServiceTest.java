package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.team.dto.TaskAttachmentDTO;
import com.teamup.server.modules.team.entity.TaskAttachment;
import com.teamup.server.modules.team.mapper.TaskAttachmentMapper;
import com.teamup.server.modules.team.service.impl.TaskAttachmentServiceImpl;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 任务附件服务单元测试
 * Requirements: 4.1, 4.2, 4.3, 4.4
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskAttachmentServiceTest {

    @Mock
    private TaskAttachmentMapper taskAttachmentMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TaskAttachmentServiceImpl taskAttachmentService;

    private User testUser;
    private TaskAttachment testAttachment;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        // Inject the mapper mock into the service
        ReflectionTestUtils.setField(taskAttachmentService, "baseMapper", taskAttachmentMapper);
        ReflectionTestUtils.setField(taskAttachmentService, "accessUrl", "http://localhost:8080/uploads");

        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testAttachment = new TaskAttachment();
        testAttachment.setId(1L);
        testAttachment.setTaskId(100L);
        testAttachment.setFileName("test.pdf");
        testAttachment.setFilePath("tasks/100/test.pdf");
        testAttachment.setFileSize(1024L);
        testAttachment.setUploadedBy(1L);
        testAttachment.setUploadedAt(LocalDateTime.now());

        testFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test content".getBytes()
        );
    }

    /**
     * 测试文件上传成功场景
     * Requirements: 4.1
     */
    @Test
    void testUploadAttachment_Success() {
        // Given
        Long taskId = 100L;
        Long userId = 1L;

        when(fileStorageService.validateFile(any(MultipartFile.class))).thenReturn(true);
        when(fileStorageService.saveFile(any(MultipartFile.class), anyString()))
            .thenReturn("tasks/100/test.pdf");
        when(taskAttachmentMapper.insert(any(TaskAttachment.class))).thenReturn(1);
        when(userMapper.selectById(userId)).thenReturn(testUser);

        // When
        TaskAttachmentDTO result = taskAttachmentService.uploadAttachment(taskId, testFile, userId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals("test.pdf", result.getFileName());
        assertEquals(userId, result.getUploadedBy());
        assertEquals("testuser", result.getUploaderName());
        assertNotNull(result.getUploadedAt());

        verify(fileStorageService).validateFile(any(MultipartFile.class));
        verify(fileStorageService).saveFile(any(MultipartFile.class), eq("tasks/100"));
        verify(taskAttachmentMapper).insert(any(TaskAttachment.class));
        verify(userMapper).selectById(userId);
    }

    /**
     * 测试文件验证失败场景
     * Requirements: 4.4
     */
    @Test
    void testUploadAttachment_ValidationFailed() {
        // Given
        Long taskId = 100L;
        Long userId = 1L;

        when(fileStorageService.validateFile(any(MultipartFile.class))).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskAttachmentService.uploadAttachment(taskId, testFile, userId);
        });

        assertTrue(exception.getMessage().contains("文件验证失败"));

        verify(fileStorageService).validateFile(any(MultipartFile.class));
        verify(fileStorageService, never()).saveFile(any(), any());
        verify(taskAttachmentMapper, never()).insert(any());
    }

    /**
     * 测试文件删除成功场景
     * Requirements: 4.3
     */
    @Test
    void testDeleteAttachment_Success() {
        // Given
        Long attachmentId = 1L;

        when(taskAttachmentMapper.selectById(attachmentId)).thenReturn(testAttachment);
        doNothing().when(fileStorageService).deleteFile(anyString());
        // Mock the removeById method from ServiceImpl
        TaskAttachmentServiceImpl spyService = spy(taskAttachmentService);
        doReturn(true).when(spyService).removeById(attachmentId);

        // When
        spyService.deleteAttachment(attachmentId);

        // Then
        verify(taskAttachmentMapper).selectById(attachmentId);
        verify(fileStorageService).deleteFile("http://localhost:8080/uploads/tasks/100/test.pdf");
        verify(spyService).removeById(attachmentId);
    }

    /**
     * 测试删除不存在的附件
     * Requirements: 4.3
     */
    @Test
    void testDeleteAttachment_NotFound() {
        // Given
        Long attachmentId = 999L;

        when(taskAttachmentMapper.selectById(attachmentId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskAttachmentService.deleteAttachment(attachmentId);
        });

        assertTrue(exception.getMessage().contains("附件不存在"));

        verify(taskAttachmentMapper).selectById(attachmentId);
        verify(fileStorageService, never()).deleteFile(any());
        verify(taskAttachmentMapper, never()).deleteById(anyLong());
    }

    /**
     * 测试获取任务附件列表
     * Requirements: 4.1
     */
    @Test
    void testGetAttachmentsByTaskId_Success() {
        // Given
        Long taskId = 100L;

        TaskAttachment attachment1 = new TaskAttachment();
        attachment1.setId(1L);
        attachment1.setTaskId(taskId);
        attachment1.setFileName("file1.pdf");
        attachment1.setFilePath("tasks/100/file1.pdf");
        attachment1.setFileSize(1024L);
        attachment1.setUploadedBy(1L);
        attachment1.setUploadedAt(LocalDateTime.now());

        TaskAttachment attachment2 = new TaskAttachment();
        attachment2.setId(2L);
        attachment2.setTaskId(taskId);
        attachment2.setFileName("file2.docx");
        attachment2.setFilePath("tasks/100/file2.docx");
        attachment2.setFileSize(2048L);
        attachment2.setUploadedBy(1L);
        attachment2.setUploadedAt(LocalDateTime.now());

        List<TaskAttachment> attachments = Arrays.asList(attachment1, attachment2);

        when(taskAttachmentMapper.selectList(any())).thenReturn(attachments);
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // When
        List<TaskAttachmentDTO> result = taskAttachmentService.getAttachmentsByTaskId(taskId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("file1.pdf", result.get(0).getFileName());
        assertEquals("file2.docx", result.get(1).getFileName());
        assertEquals("testuser", result.get(0).getUploaderName());

        verify(taskAttachmentMapper).selectList(any());
        verify(userMapper, times(2)).selectById(1L);
    }

    /**
     * 测试获取空任务的附件列表
     * Requirements: 4.1
     */
    @Test
    void testGetAttachmentsByTaskId_EmptyList() {
        // Given
        Long taskId = 100L;

        when(taskAttachmentMapper.selectList(any())).thenReturn(Arrays.asList());

        // When
        List<TaskAttachmentDTO> result = taskAttachmentService.getAttachmentsByTaskId(taskId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(taskAttachmentMapper).selectList(any());
    }

    /**
     * 测试文件下载成功场景
     * Requirements: 4.2
     */
    @Test
    void testDownloadAttachment_Success() {
        // Given
        Long attachmentId = 1L;
        byte[] fileContent = "test content".getBytes();
        Resource mockResource = new ByteArrayResource(fileContent);

        when(taskAttachmentMapper.selectById(attachmentId)).thenReturn(testAttachment);
        when(fileStorageService.loadFile("tasks/100/test.pdf")).thenReturn(mockResource);

        // When
        Resource result = taskAttachmentService.downloadAttachment(attachmentId);

        // Then
        assertNotNull(result);
        assertEquals(mockResource, result);

        verify(taskAttachmentMapper).selectById(attachmentId);
        verify(fileStorageService).loadFile("tasks/100/test.pdf");
    }

    /**
     * 测试下载不存在的附件
     * Requirements: 4.2
     */
    @Test
    void testDownloadAttachment_NotFound() {
        // Given
        Long attachmentId = 999L;

        when(taskAttachmentMapper.selectById(attachmentId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskAttachmentService.downloadAttachment(attachmentId);
        });

        assertTrue(exception.getMessage().contains("附件不存在"));

        verify(taskAttachmentMapper).selectById(attachmentId);
        verify(fileStorageService, never()).loadFile(any());
    }

    /**
     * 测试上传大文件验证失败
     * Requirements: 4.4
     */
    @Test
    void testUploadAttachment_FileTooLarge() {
        // Given
        Long taskId = 100L;
        Long userId = 1L;

        // Create a large file mock
        MultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            new byte[11 * 1024 * 1024] // 11MB
        );

        when(fileStorageService.validateFile(any(MultipartFile.class))).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskAttachmentService.uploadAttachment(taskId, largeFile, userId);
        });

        assertTrue(exception.getMessage().contains("文件验证失败"));

        verify(fileStorageService).validateFile(any(MultipartFile.class));
        verify(fileStorageService, never()).saveFile(any(), any());
    }

    /**
     * 测试上传附件时用户不存在
     * Requirements: 4.1
     */
    @Test
    void testUploadAttachment_UserNotFound() {
        // Given
        Long taskId = 100L;
        Long userId = 999L;

        when(fileStorageService.validateFile(any(MultipartFile.class))).thenReturn(true);
        when(fileStorageService.saveFile(any(MultipartFile.class), anyString()))
            .thenReturn("tasks/100/test.pdf");
        when(taskAttachmentMapper.insert(any(TaskAttachment.class))).thenReturn(1);
        when(userMapper.selectById(userId)).thenReturn(null);

        // When
        TaskAttachmentDTO result = taskAttachmentService.uploadAttachment(taskId, testFile, userId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(userId, result.getUploadedBy());
        assertNull(result.getUploaderName()); // User not found, so name should be null

        verify(fileStorageService).validateFile(any(MultipartFile.class));
        verify(fileStorageService).saveFile(any(MultipartFile.class), anyString());
        verify(taskAttachmentMapper).insert(any(TaskAttachment.class));
        verify(userMapper).selectById(userId);
    }
}
