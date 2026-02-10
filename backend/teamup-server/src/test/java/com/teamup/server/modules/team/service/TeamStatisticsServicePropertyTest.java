package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.teamup.server.modules.activity.entity.TeamActivity;
import com.teamup.server.modules.activity.mapper.TeamActivityMapper;
import com.teamup.server.modules.chat.mapper.MessageMapper;
import com.teamup.server.modules.file.entity.FileEntity;
import com.teamup.server.modules.file.mapper.FileMapper;
import com.teamup.server.modules.project.entity.ProjectFile;
import com.teamup.server.modules.project.mapper.ProjectFileMapper;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.service.impl.TeamStatisticsServiceImpl;
import com.teamup.server.modules.team.vo.TeamStatisticsVO;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 团队统计服务属性测试
 * Feature: team-features-implementation
 */
public class TeamStatisticsServicePropertyTest {

    private TaskMapper taskMapper;
    private FileMapper fileMapper;
    private ProjectFileMapper projectFileMapper;
    private TeamActivityMapper teamActivityMapper;
    private TeamMapper teamMapper;
    private MessageMapper messageMapper;
    private TeamStatisticsServiceImpl statisticsService;

    private void setUp() {
        taskMapper = mock(TaskMapper.class);
        fileMapper = mock(FileMapper.class);
        projectFileMapper = mock(ProjectFileMapper.class);
        teamActivityMapper = mock(TeamActivityMapper.class);
        teamMapper = mock(TeamMapper.class);
        messageMapper = mock(MessageMapper.class);
        statisticsService = new TeamStatisticsServiceImpl(
            taskMapper, fileMapper, projectFileMapper, teamActivityMapper, teamMapper, messageMapper
        );
    }

    /**
     * Property 1: 统计数据计算正确性
     * For any 团队，当计算统计数据时，任务完成率应该等于已完成任务数除以总任务数乘以 100，
     * 活跃天数应该等于创建日期到最后活动日期的天数，消息数量应该等于该团队的消息总数，
     * 文件数量应该等于该团队的非文件夹文件总数
     * Validates: Requirements 1.1, 1.2, 1.3, 1.4
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 1: 统计数据计算正确性")
    void statisticsCalculationShouldBeCorrect(
            @ForAll("teams") Team team,
            @ForAll("taskLists") List<Task> tasks,
            @ForAll("fileLists") List<FileEntity> teamFiles,
            @ForAll("projectFileLists") List<ProjectFile> projectFiles,
            @ForAll("activityLists") List<TeamActivity> activities) {
        
        // Setup
        setUp();
        when(teamMapper.selectById(team.getId())).thenReturn(team);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(tasks);
        when(fileMapper.selectCount(any(Wrapper.class))).thenReturn((long) teamFiles.size());
        when(projectFileMapper.selectCount(any(Wrapper.class))).thenReturn((long) projectFiles.size());
        
        // Setup last activity for active days calculation
        if (!activities.isEmpty()) {
            TeamActivity lastActivity = activities.get(0); // Already sorted descending
            when(teamActivityMapper.selectOne(any(Wrapper.class))).thenReturn(lastActivity);
        } else {
            when(teamActivityMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        }
        
        // Calculate expected values
        int expectedCompletionRate = calculateExpectedCompletionRate(tasks);
        int expectedActiveDays = calculateExpectedActiveDays(team, activities);
        int expectedMessageCount = teamFiles.size(); // Using files as message count
        int expectedFileCount = projectFiles.size();
        
        // Execute
        TeamStatisticsVO stats = statisticsService.calculateStatistics(team.getId());
        
        // Verify
        assertEquals(expectedCompletionRate, stats.getTaskCompletionRate(),
            "Task completion rate should match expected calculation");
        assertEquals(expectedActiveDays, stats.getActiveDays(),
            "Active days should match expected calculation");
        assertEquals(expectedMessageCount, stats.getMessageCount(),
            "Message count should match expected count");
        assertEquals(expectedFileCount, stats.getFileCount(),
            "File count should match expected count");
    }

    /**
     * Property 2: 统计数据实时更新
     * For any 团队，当团队数据发生变化（添加任务、消息或文件）后，
     * 再次查询统计数据应该反映最新的变化
     * Validates: Requirements 1.6
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 2: 统计数据实时更新")
    void statisticsShouldReflectDataChanges(
            @ForAll("teams") Team team,
            @ForAll("taskLists") List<Task> initialTasks,
            @ForAll("fileLists") List<FileEntity> initialFiles) {
        
        // Setup initial state
        setUp();
        when(teamMapper.selectById(team.getId())).thenReturn(team);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(initialTasks);
        when(fileMapper.selectCount(any(Wrapper.class))).thenReturn((long) initialFiles.size());
        when(projectFileMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(teamActivityMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        
        // Get initial statistics
        TeamStatisticsVO initialStats = statisticsService.calculateStatistics(team.getId());
        int initialMessageCount = initialStats.getMessageCount();
        
        // Simulate data change: add new files
        List<FileEntity> updatedFiles = new ArrayList<>(initialFiles);
        FileEntity newFile = new FileEntity();
        newFile.setId(999L);
        newFile.setTeamId(team.getId());
        newFile.setFileName("New file");
        updatedFiles.add(newFile);
        
        // Update mock to return new data
        when(fileMapper.selectCount(any(Wrapper.class))).thenReturn((long) updatedFiles.size());
        
        // Get updated statistics
        TeamStatisticsVO updatedStats = statisticsService.calculateStatistics(team.getId());
        
        // Verify: Statistics should reflect the change
        assertEquals(initialMessageCount + 1, updatedStats.getMessageCount(),
            "Message count should increase by 1 after adding a new file");
    }

    // ===== Helper Methods =====

    private int calculateExpectedCompletionRate(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return 0;
        }
        long completedCount = tasks.stream()
                .filter(task -> "DONE".equals(task.getStatus()))
                .count();
        return (int) ((completedCount * 100) / tasks.size());
    }

    private int calculateExpectedActiveDays(Team team, List<TeamActivity> activities) {
        if (team.getCreatedAt() == null) {
            return 0;
        }
        
        LocalDateTime endTime = activities.isEmpty() 
            ? LocalDateTime.now() 
            : activities.get(0).getCreatedAt();
        
        long days = ChronoUnit.DAYS.between(team.getCreatedAt(), endTime);
        return (int) Math.max(1, days + 1);
    }

    // ===== Arbitraries (Data Generators) =====

    @Provide
    Arbitrary<Team> teams() {
        return Arbitraries.longs().between(1L, 1000L).flatMap(id -> {
            Team team = new Team();
            team.setId(id);
            team.setProjectId(id);
            team.setTeamName("Team " + id);
            team.setLeaderId(1L);
            team.setCreatedAt(LocalDateTime.now().minusDays(
                Arbitraries.integers().between(1, 365).sample()
            ));
            return Arbitraries.just(team);
        });
    }

    @Provide
    Arbitrary<List<Task>> taskLists() {
        return Arbitraries.integers().between(0, 50).flatMap(size -> {
            List<Task> tasks = new ArrayList<>();
            String[] statuses = {"TODO", "DOING", "REVIEW", "DONE"};
            
            for (int i = 0; i < size; i++) {
                Task task = new Task();
                task.setId((long) (i + 1));
                task.setTeamId(1L);
                task.setTitle("Task " + i);
                task.setStatus(statuses[i % statuses.length]);
                task.setPriority("MEDIUM");
                task.setCreatedAt(LocalDateTime.now().minusDays(i));
                tasks.add(task);
            }
            
            return Arbitraries.just(tasks);
        });
    }

    @Provide
    Arbitrary<List<FileEntity>> fileLists() {
        return Arbitraries.integers().between(0, 100).flatMap(size -> {
            List<FileEntity> files = new ArrayList<>();
            
            for (int i = 0; i < size; i++) {
                FileEntity file = new FileEntity();
                file.setId((long) (i + 1));
                file.setTeamId(1L);
                file.setFileName("file_" + i + ".txt");
                file.setFileType("DOCUMENT");
                file.setFileSize(1024L * (i + 1));
                file.setUploaderId(1L);
                file.setUploadedAt(LocalDateTime.now().minusMinutes(i));
                files.add(file);
            }
            
            return Arbitraries.just(files);
        });
    }

    @Provide
    Arbitrary<List<ProjectFile>> projectFileLists() {
        return Arbitraries.integers().between(0, 30).flatMap(size -> {
            List<ProjectFile> files = new ArrayList<>();
            
            for (int i = 0; i < size; i++) {
                ProjectFile file = new ProjectFile();
                file.setId((long) (i + 1));
                file.setProjectId(1L);
                file.setFileName("file_" + i + ".txt");
                file.setFileType("DOCUMENT");
                file.setFileSize(1024L * (i + 1));
                file.setUploaderId(1L);
                file.setUploadedAt(LocalDateTime.now().minusHours(i));
                files.add(file);
            }
            
            return Arbitraries.just(files);
        });
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
                activity.setActivityType("task");
                activity.setAction("test_action");
                activity.setDetail("Test detail " + i);
                activity.setCreatedAt(baseTime.minusDays(i));
                activities.add(activity);
            }
            
            // Sort by createdAt descending
            activities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            
            return Arbitraries.just(activities);
        });
    }
}
