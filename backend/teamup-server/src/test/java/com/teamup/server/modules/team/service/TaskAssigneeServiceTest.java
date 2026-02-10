package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.team.dto.TaskAssigneeDTO;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.mapper.TaskAssigneeMapper;
import com.teamup.server.modules.team.service.impl.TaskAssigneeServiceImpl;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 任务负责人服务单元测试
 * Requirements: 1.1, 1.2
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskAssigneeServiceTest {

    @Mock
    private TaskAssigneeMapper taskAssigneeMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private TaskAssigneeServiceImpl taskAssigneeService;

    private User testUser;
    private UserProfile testProfile;
    private TaskAssignee testAssignee;

    @BeforeEach
    void setUp() {
        // Inject the mapper mock into the service
        ReflectionTestUtils.setField(taskAssigneeService, "baseMapper", taskAssigneeMapper);

        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testProfile = new UserProfile();
        testProfile.setUserId(1L);
        testProfile.setAvatarUrl("avatar.jpg");

        testAssignee = new TaskAssignee();
        testAssignee.setId(1L);
        testAssignee.setTaskId(100L);
        testAssignee.setUserId(1L);
        testAssignee.setAssignedAt(LocalDateTime.now());
    }

    /**
     * 测试添加负责人成功场景
     * Requirements: 1.1
     */
    @Test
    void testAddAssignee_Success() {
        // Given
        Long taskId = 100L;
        Long userId = 1L;

        when(taskAssigneeMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(taskAssigneeMapper.insert(any(TaskAssignee.class))).thenReturn(1);
        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userProfileMapper.selectOne(any())).thenReturn(testProfile);

        // When
        TaskAssigneeDTO result = taskAssigneeService.addAssignee(taskId, userId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(userId, result.getUserId());
        assertEquals("testuser", result.getUserName());
        assertEquals("avatar.jpg", result.getAvatar());
        assertNotNull(result.getAssignedAt());

        verify(taskAssigneeMapper).insert(any(TaskAssignee.class));
        verify(userMapper).selectById(userId);
    }

    /**
     * 测试重复添加负责人的边缘情况
     * Requirements: 1.1
     */
    @Test
    void testAddAssignee_AlreadyExists() {
        // Given
        Long taskId = 100L;
        Long userId = 1L;

        when(taskAssigneeMapper.selectOne(any(), anyBoolean())).thenReturn(testAssignee);
        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userProfileMapper.selectOne(any())).thenReturn(testProfile);

        // When
        TaskAssigneeDTO result = taskAssigneeService.addAssignee(taskId, userId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(userId, result.getUserId());

        // Verify insert was NOT called since assignee already exists
        verify(taskAssigneeMapper, never()).insert(any(TaskAssignee.class));
    }

    /**
     * 测试移除负责人成功场景
     * Requirements: 1.2
     */
    @Test
    void testRemoveAssignee_Success() {
        // Given
        Long taskId = 100L;
        Long userId = 1L;

        when(taskAssigneeMapper.delete(any())).thenReturn(1);

        // When
        taskAssigneeService.removeAssignee(taskId, userId);

        // Then
        verify(taskAssigneeMapper).delete(any());
    }

    /**
     * 测试获取任务负责人列表
     * Requirements: 1.1
     */
    @Test
    void testGetAssigneesByTaskId_Success() {
        // Given
        Long taskId = 100L;

        TaskAssignee assignee1 = new TaskAssignee();
        assignee1.setId(1L);
        assignee1.setTaskId(taskId);
        assignee1.setUserId(1L);
        assignee1.setAssignedAt(LocalDateTime.now());

        TaskAssignee assignee2 = new TaskAssignee();
        assignee2.setId(2L);
        assignee2.setTaskId(taskId);
        assignee2.setUserId(2L);
        assignee2.setAssignedAt(LocalDateTime.now());

        List<TaskAssignee> assignees = Arrays.asList(assignee1, assignee2);

        when(taskAssigneeMapper.selectList(any())).thenReturn(assignees);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectById(2L)).thenReturn(testUser);
        when(userProfileMapper.selectOne(any())).thenReturn(testProfile);

        // When
        List<TaskAssigneeDTO> result = taskAssigneeService.getAssigneesByTaskId(taskId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(2L, result.get(1).getUserId());

        verify(taskAssigneeMapper).selectList(any());
        verify(userMapper, times(2)).selectById(any());
    }

    /**
     * 测试获取空任务的负责人列表
     * Requirements: 1.5
     */
    @Test
    void testGetAssigneesByTaskId_EmptyList() {
        // Given
        Long taskId = 100L;

        when(taskAssigneeMapper.selectList(any())).thenReturn(Arrays.asList());

        // When
        List<TaskAssigneeDTO> result = taskAssigneeService.getAssigneesByTaskId(taskId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(taskAssigneeMapper).selectList(any());
    }

    /**
     * 测试添加负责人时用户不存在的情况
     * Requirements: 1.1
     */
    @Test
    void testAddAssignee_UserNotFound() {
        // Given
        Long taskId = 100L;
        Long userId = 999L;

        when(taskAssigneeMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(taskAssigneeMapper.insert(any(TaskAssignee.class))).thenReturn(1);
        when(userMapper.selectById(userId)).thenReturn(null);

        // When
        TaskAssigneeDTO result = taskAssigneeService.addAssignee(taskId, userId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(userId, result.getUserId());
        assertNull(result.getUserName()); // User not found, so name should be null

        verify(taskAssigneeMapper).insert(any(TaskAssignee.class));
        verify(userMapper).selectById(userId);
    }
}
