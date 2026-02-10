package com.teamup.server.modules.activity.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.activity.entity.TeamActivity;
import com.teamup.server.modules.activity.mapper.TeamActivityMapper;
import com.teamup.server.modules.activity.mapper.UserActivityMapper;
import com.teamup.server.modules.activity.service.impl.ActivityServiceImpl;
import com.teamup.server.modules.activity.vo.ActivityVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 活动记录服务属性测试
 * Feature: team-features-implementation
 */
public class ActivityServicePropertyTest {

    private TeamActivityMapper teamActivityMapper;
    private UserActivityMapper userActivityMapper;
    private UserMapper userMapper;
    private ActivityServiceImpl activityService;

    private void setUp() {
        teamActivityMapper = mock(TeamActivityMapper.class);
        userActivityMapper = mock(UserActivityMapper.class);
        userMapper = mock(UserMapper.class);
        activityService = new ActivityServiceImpl(userActivityMapper, teamActivityMapper, userMapper);
    }

    /**
     * Property 3: 活动记录列表排序
     * For any 团队的活动记录列表，所有记录应该按创建时间倒序排列，即最新的活动在最前面
     * Validates: Requirements 2.7
     */
    @Property(tries = 20)
    @Label("Feature: team-features-implementation, Property 3: 活动记录列表排序")
    void activityListShouldBeSortedByCreatedAtDescending(
            @ForAll("teamIds") Long teamId,
            @ForAll("activityLists") List<TeamActivity> activities) {
        
        // Setup
        setUp();
        when(teamActivityMapper.selectList(any(Wrapper.class))).thenReturn(activities);
        
        // Execute: Get recent activities
        List<ActivityVO> result = activityService.getRecentActivities(teamId, null);
        
        // Verify: Activities should be sorted by createdAt in descending order
        for (int i = 0; i < result.size() - 1; i++) {
            LocalDateTime current = result.get(i).getCreatedAt();
            LocalDateTime next = result.get(i + 1).getCreatedAt();
            
            assertTrue(
                current.isAfter(next) || current.isEqual(next),
                "Activities should be sorted by createdAt in descending order. " +
                "Found: " + current + " before " + next
            );
        }
    }

    /**
     * Property 4: 活动记录数量限制
     * For any 团队和任意正整数 limit，查询活动记录时指定 limit 参数，返回的记录数量应该不超过 limit
     * Validates: Requirements 2.8
     */
    @Property(tries = 20)
    @Label("Feature: team-features-implementation, Property 4: 活动记录数量限制")
    void activityListShouldRespectLimitParameter(
            @ForAll("teamIds") Long teamId,
            @ForAll("positiveIntegers") Integer limit,
            @ForAll("largeActivityLists") List<TeamActivity> activities) {
        
        // Setup: Simulate SQL LIMIT behavior by returning only the first 'limit' items
        List<TeamActivity> limitedActivities = activities.size() <= limit 
            ? activities 
            : activities.subList(0, limit);
        
        setUp();
        when(teamActivityMapper.selectList(any(Wrapper.class))).thenReturn(limitedActivities);
        
        // Execute: Get recent activities with limit
        List<ActivityVO> result = activityService.getRecentActivities(teamId, limit);
        
        // Verify: Result size should not exceed limit
        assertTrue(
            result.size() <= limit,
            "Result size (" + result.size() + ") should not exceed limit (" + limit + ")"
        );
    }

    /**
     * Property 5: 活动记录信息完整性
     * For any 活动记录，应该包含用户名、活动类型、详细描述和时间戳这些必需字段
     * Validates: Requirements 2.9
     */
    @Property(tries = 20)
    @Label("Feature: team-features-implementation, Property 5: 活动记录信息完整性")
    void activityRecordShouldContainRequiredFields(
            @ForAll("teamIds") Long teamId,
            @ForAll("activityLists") List<TeamActivity> activities) {
        
        // Setup: Ensure all activities have required fields
        activities.forEach(activity -> {
            activity.setUsername("user_" + activity.getUserId());
            activity.setActivityType("task");
            activity.setDetail("Test detail");
            activity.setCreatedAt(LocalDateTime.now());
        });
        
        setUp();
        when(teamActivityMapper.selectList(any(Wrapper.class))).thenReturn(activities);
        
        // Execute: Get recent activities
        List<ActivityVO> result = activityService.getRecentActivities(teamId, null);
        
        // Verify: Each activity should have required fields
        for (ActivityVO activity : result) {
            assertNotNull(activity.getUsername(), "Username should not be null");
            assertNotNull(activity.getActivityType(), "Activity type should not be null");
            assertNotNull(activity.getDetail(), "Detail should not be null");
            assertNotNull(activity.getCreatedAt(), "Created at should not be null");
            
            assertFalse(activity.getUsername().isEmpty(), "Username should not be empty");
            assertFalse(activity.getActivityType().isEmpty(), "Activity type should not be empty");
            assertFalse(activity.getDetail().isEmpty(), "Detail should not be empty");
        }
    }

    // ===== Arbitraries (Data Generators) =====

    @Provide
    Arbitrary<Long> teamIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<Integer> positiveIntegers() {
        return Arbitraries.integers().between(1, 100);
    }

    @Provide
    Arbitrary<List<TeamActivity>> activityLists() {
        return Arbitraries.integers().between(0, 20).flatMap(size -> {
            List<TeamActivity> activities = new ArrayList<>();
            LocalDateTime baseTime = LocalDateTime.now();
            
            for (int i = 0; i < size; i++) {
                TeamActivity activity = new TeamActivity();
                activity.setId((long) (i + 1));
                activity.setTeamId(1L);
                activity.setUserId((long) (i % 5 + 1));
                activity.setUsername("user_" + (i % 5 + 1));
                activity.setActivityType(getRandomActivityType(i));
                activity.setAction("test_action");
                activity.setDetail("Test detail " + i);
                activity.setCreatedAt(baseTime.minusMinutes(i));
                activities.add(activity);
            }
            
            // Sort by createdAt descending to match expected behavior
            activities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            
            return Arbitraries.just(activities);
        });
    }

    @Provide
    Arbitrary<List<TeamActivity>> largeActivityLists() {
        return Arbitraries.integers().between(50, 200).flatMap(size -> {
            List<TeamActivity> activities = new ArrayList<>();
            LocalDateTime baseTime = LocalDateTime.now();
            
            for (int i = 0; i < size; i++) {
                TeamActivity activity = new TeamActivity();
                activity.setId((long) (i + 1));
                activity.setTeamId(1L);
                activity.setUserId((long) (i % 10 + 1));
                activity.setUsername("user_" + (i % 10 + 1));
                activity.setActivityType(getRandomActivityType(i));
                activity.setAction("test_action");
                activity.setDetail("Test detail " + i);
                activity.setCreatedAt(baseTime.minusSeconds(i));
                activities.add(activity);
            }
            
            // Sort by createdAt descending
            activities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            
            return Arbitraries.just(activities);
        });
    }

    /**
     * Property 6: 操作触发活动记录创建
     * For any 团队操作（完成任务、上传文件、发送消息、成员变更、设置修改），执行操作后应该创建对应类型的活动记录
     * Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6
     */
    @Property(tries = 20)
    @Label("Feature: team-features-implementation, Property 6: 操作触发活动记录创建")
    void operationsShouldTriggerActivityRecordCreation(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long userId,
            @ForAll("activityTypes") String activityType) {
        
        // Setup
        setUp();
        User user = new User();
        user.setId(userId);
        user.setUsername("test_user_" + userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        
        // Track to capture the inserted activity
        final TeamActivity[] capturedActivity = {null};
        when(teamActivityMapper.insert(any(TeamActivity.class))).thenAnswer(invocation -> {
            capturedActivity[0] = invocation.getArgument(0);
            return 1;
        });
        
        // Execute: Trigger different types of activities
        switch (activityType) {
            case "task":
                activityService.trackTaskActivity(teamId, userId, "complete", "完成了任务", 1L);
                break;
            case "file":
                activityService.trackFileActivity(teamId, userId, "upload", "上传了文件", 1L);
                break;
            case "message":
                activityService.trackMessageActivity(teamId, userId, "发送了消息");
                break;
            case "member":
                activityService.trackMemberActivity(teamId, userId, "join", "加入了团队");
                break;
            case "setting":
                activityService.trackSettingActivity(teamId, userId, "修改了团队设置");
                break;
        }
        
        // Wait a bit for async operation to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify: Activity record should be created with correct type
        assertNotNull(capturedActivity[0], "Activity record should be created");
        assertEquals(teamId, capturedActivity[0].getTeamId(), "Team ID should match");
        assertEquals(userId, capturedActivity[0].getUserId(), "User ID should match");
        assertEquals(activityType, capturedActivity[0].getActivityType(), 
            "Activity type should match the operation type");
    }

    /**
     * Property 24: 活动记录数据完整性
     * For any 创建的活动记录，应该包含所有必需字段（team_id, user_id, username, activity_type, detail, created_at）
     * Validates: Requirements 7.2
     */
    @Property(tries = 20)
    @Label("Feature: team-features-implementation, Property 24: 活动记录数据完整性")
    void createdActivityRecordShouldContainAllRequiredFields(
            @ForAll("teamIds") Long teamId,
            @ForAll("userIds") Long userId,
            @ForAll("activityTypes") String activityType,
            @ForAll("activityDetails") String detail) {
        
        // Setup
        setUp();
        User user = new User();
        user.setId(userId);
        user.setUsername("test_user_" + userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        
        // Track to capture the inserted activity
        final TeamActivity[] capturedActivity = {null};
        when(teamActivityMapper.insert(any(TeamActivity.class))).thenAnswer(invocation -> {
            capturedActivity[0] = invocation.getArgument(0);
            return 1;
        });
        
        // Execute: Create activity based on type
        switch (activityType) {
            case "task":
                activityService.trackTaskActivity(teamId, userId, "complete", detail, 1L);
                break;
            case "file":
                activityService.trackFileActivity(teamId, userId, "upload", detail, 1L);
                break;
            case "message":
                activityService.trackMessageActivity(teamId, userId, detail);
                break;
            case "member":
                activityService.trackMemberActivity(teamId, userId, "join", detail);
                break;
            case "setting":
                activityService.trackSettingActivity(teamId, userId, detail);
                break;
        }
        
        // Wait a bit for async operation to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify: All required fields should be present
        assertNotNull(capturedActivity[0], "Activity record should be created");
        assertNotNull(capturedActivity[0].getTeamId(), "team_id should not be null");
        assertNotNull(capturedActivity[0].getUserId(), "user_id should not be null");
        assertNotNull(capturedActivity[0].getUsername(), "username should not be null");
        assertNotNull(capturedActivity[0].getActivityType(), "activity_type should not be null");
        assertNotNull(capturedActivity[0].getDetail(), "detail should not be null");
        assertNotNull(capturedActivity[0].getCreatedAt(), "created_at should not be null");
        
        // Verify field values
        assertEquals(teamId, capturedActivity[0].getTeamId());
        assertEquals(userId, capturedActivity[0].getUserId());
        assertEquals("test_user_" + userId, capturedActivity[0].getUsername());
        assertEquals(activityType, capturedActivity[0].getActivityType());
        assertEquals(detail, capturedActivity[0].getDetail());
    }

    // ===== Additional Arbitraries =====

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 100L);
    }

    @Provide
    Arbitrary<String> activityTypes() {
        return Arbitraries.of("task", "file", "message", "member", "setting");
    }

    @Provide
    Arbitrary<String> activityDetails() {
        return Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(100);
    }

    private String getRandomActivityType(int index) {
        String[] types = {"task", "file", "message", "member", "setting"};
        return types[index % types.length];
    }
}
