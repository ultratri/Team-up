package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.team.dto.TaskAssigneeDTO;
import com.teamup.server.modules.team.entity.TaskAssignee;
import com.teamup.server.modules.team.mapper.TaskAssigneeMapper;
import com.teamup.server.modules.team.service.impl.TaskAssigneeServiceImpl;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * 任务负责人服务属性测试
 * Feature: task-board-enhancement
 * 
 * 使用模拟对象测试服务层逻辑
 */
public class TaskAssigneeServicePropertyTest {

    /**
     * Property 1: 负责人分配和移除的一致性
     * For any task and user, when an assignee is added to a task, the assignee should appear 
     * in the task_assignees table; when removed, the assignee should no longer appear in the table.
     * Validates: Requirements 1.1, 1.2
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 1: 负责人分配和移除的一致性")
    void assigneeAddAndRemoveShouldBeConsistent(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll @IntRange(min = 1, max = 1000) int userIdSeed) {
        
        Long taskId = (long) taskIdSeed;
        Long userId = (long) userIdSeed;
        
        // Setup mocks
        TaskAssigneeMapper taskAssigneeMapper = mock(TaskAssigneeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        TaskAssigneeServiceImpl taskAssigneeService = new TaskAssigneeServiceImpl(userMapper, userProfileMapper);
        
        // Inject the mocked mapper into the base class using reflection
        ReflectionTestUtils.setField(taskAssigneeService, "baseMapper", taskAssigneeMapper);
        
        // Mock user data
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setAvatarUrl("avatar_" + userId + ".jpg");
        when(userProfileMapper.selectOne(any(Wrapper.class))).thenReturn(profile);
        
        // Track assignees in memory to simulate database
        List<TaskAssignee> assigneeList = new ArrayList<>();
        
        // Mock selectOne for checking existing assignee (initially null)
        when(taskAssigneeMapper.selectOne(any(Wrapper.class), anyBoolean())).thenAnswer(invocation -> {
            return assigneeList.stream()
                .filter(a -> a.getTaskId().equals(taskId) && a.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        });
        
        // Mock insert behavior
        when(taskAssigneeMapper.insert(any(TaskAssignee.class))).thenAnswer(invocation -> {
            TaskAssignee assignee = invocation.getArgument(0);
            assignee.setId((long) (assigneeList.size() + 1));
            assigneeList.add(assignee);
            return 1;
        });
        
        // Mock select behavior - return assignees for this task
        when(taskAssigneeMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return assigneeList.stream()
                .filter(a -> a.getTaskId().equals(taskId))
                .collect(Collectors.toList());
        });
        
        // Mock delete behavior
        when(taskAssigneeMapper.delete(any(Wrapper.class))).thenAnswer(invocation -> {
            int sizeBefore = assigneeList.size();
            assigneeList.removeIf(a -> a.getTaskId().equals(taskId) && a.getUserId().equals(userId));
            return sizeBefore - assigneeList.size();
        });
        
        // Execute: Add assignee
        TaskAssigneeDTO addedAssignee = taskAssigneeService.addAssignee(taskId, userId);
        
        // Verify: Assignee should be added
        assertNotNull(addedAssignee, "Added assignee should not be null");
        assertEquals(taskId, addedAssignee.getTaskId(), "Task ID should match");
        assertEquals(userId, addedAssignee.getUserId(), "User ID should match");
        assertNotNull(addedAssignee.getAssignedAt(), "Assigned time should be set");
        
        // Verify: Assignee should appear in the list
        List<TaskAssigneeDTO> assignees = taskAssigneeService.getAssigneesByTaskId(taskId);
        assertTrue(assignees.stream().anyMatch(a -> a.getUserId().equals(userId)),
            "Assignee should appear in the task assignees list after being added");
        
        // Execute: Remove assignee
        taskAssigneeService.removeAssignee(taskId, userId);
        
        // Verify: Assignee should no longer appear in the list
        List<TaskAssigneeDTO> assigneesAfterRemoval = taskAssigneeService.getAssigneesByTaskId(taskId);
        assertFalse(assigneesAfterRemoval.stream().anyMatch(a -> a.getUserId().equals(userId)),
            "Assignee should not appear in the task assignees list after being removed");
    }

    /**
     * Property 2: 多个负责人管理
     * For any task, the system should allow the task to have zero, one, or multiple assignees.
     * Validates: Requirements 1.5
     */
    @Property(tries = 20)
    @Label("Feature: task-board-enhancement, Property 2: 多个负责人管理")
    void taskCanHaveMultipleAssignees(
            @ForAll @IntRange(min = 1, max = 10000) int taskIdSeed,
            @ForAll("userIdLists") List<Long> userIds) {
        
        Long taskId = (long) taskIdSeed;
        
        // Setup mocks
        TaskAssigneeMapper taskAssigneeMapper = mock(TaskAssigneeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        TaskAssigneeServiceImpl taskAssigneeService = new TaskAssigneeServiceImpl(userMapper, userProfileMapper);
        
        // Inject the mocked mapper into the base class using reflection
        ReflectionTestUtils.setField(taskAssigneeService, "baseMapper", taskAssigneeMapper);
        
        // Track assignees in memory
        List<TaskAssignee> assigneeList = new ArrayList<>();
        
        // Mock user data for all users
        for (Long userId : userIds) {
            User user = new User();
            user.setId(userId);
            user.setUsername("user_" + userId);
            when(userMapper.selectById(userId)).thenReturn(user);
            
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setAvatarUrl("avatar_" + userId + ".jpg");
            when(userProfileMapper.selectOne(any(Wrapper.class))).thenReturn(profile);
        }
        
        // Mock selectOne for checking existing assignee (always return null for new assignees)
        when(taskAssigneeMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(null);
        
        // Mock insert behavior
        when(taskAssigneeMapper.insert(any(TaskAssignee.class))).thenAnswer(invocation -> {
            TaskAssignee assignee = invocation.getArgument(0);
            assignee.setId((long) (assigneeList.size() + 1));
            assigneeList.add(assignee);
            return 1;
        });
        
        // Mock select behavior
        when(taskAssigneeMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            return assigneeList.stream()
                .filter(a -> a.getTaskId().equals(taskId))
                .collect(Collectors.toList());
        });
        
        // Execute: Add all assignees
        for (Long userId : userIds) {
            taskAssigneeService.addAssignee(taskId, userId);
        }
        
        // Verify: All assignees should be present
        List<TaskAssigneeDTO> assignees = taskAssigneeService.getAssigneesByTaskId(taskId);
        assertEquals(userIds.size(), assignees.size(),
            "Number of assignees should match the number of users added");
        
        for (Long userId : userIds) {
            assertTrue(assignees.stream().anyMatch(a -> a.getUserId().equals(userId)),
                "Each added user should appear in the assignees list");
        }
    }

    // ===== Arbitraries (Data Generators) =====

    @Provide
    Arbitrary<List<Long>> userIdLists() {
        return Arbitraries.integers().between(0, 10).flatMap(size -> {
            List<Long> userIds = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                userIds.add((long) (i + 1));
            }
            return Arbitraries.just(userIds);
        });
    }
}
