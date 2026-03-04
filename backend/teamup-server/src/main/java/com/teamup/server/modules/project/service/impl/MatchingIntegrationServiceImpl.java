package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.evaluation.mapper.EvaluationMapper;
import com.teamup.server.modules.mentor.mapper.MentorPerformanceMapper;
import com.teamup.server.modules.mentor.mapper.MentorMemberEvaluationMapper;
import com.teamup.server.modules.newbie.entity.SkillCertification;
import com.teamup.server.modules.newbie.mapper.SkillCertificationMapper;
import com.teamup.server.modules.project.client.MatchingFeignClient;
import com.teamup.server.modules.project.dto.matching.*;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectApplication;
import com.teamup.server.modules.project.entity.ProjectSkillRequirement;
import com.teamup.server.modules.project.entity.ProjectTimeSlot;
import com.teamup.server.modules.project.mapper.ProjectApplicationMapper;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.project.mapper.ProjectSkillRequirementMapper;
import com.teamup.server.modules.project.mapper.ProjectTimeSlotMapper;
import com.teamup.server.modules.project.service.MatchingIntegrationService;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.entity.UserTag;
import com.teamup.server.modules.tag.mapper.TagMapper;
import com.teamup.server.modules.tag.mapper.UserTagMapper;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.team.service.TaskAssigneeService;
import com.teamup.server.modules.user.entity.*;
import com.teamup.server.modules.user.mapper.*;
import com.teamup.server.modules.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingIntegrationServiceImpl implements MatchingIntegrationService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final ProfileService profileService;
    private final MatchingFeignClient matchingClient;
    private final ProjectSkillRequirementMapper skillRequirementMapper;
    private final ProjectTimeSlotMapper projectTimeSlotMapper;
    private final UserInterestMapper userInterestMapper;
    private final UserAvailabilityMapper userAvailabilityMapper;
    private final UserTagMapper userTagMapper;
    private final TagMapper tagMapper;
    private final CollaborationHistoryMapper collaborationHistoryMapper;
    private final SkillCertificationMapper skillCertificationMapper;
    private final EvaluationMapper evaluationMapper;
    private final MentorMemberEvaluationMapper mentorMemberEvaluationMapper;
    private final ProjectApplicationMapper projectApplicationMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserCreditMapper userCreditMapper;
    private final MentorPerformanceMapper performanceMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamService teamService;
    private final TaskMapper taskMapper;
    private final TaskAssigneeService taskAssigneeService;
    private final com.teamup.server.modules.user.service.ProjectHistoryService projectHistoryService;

    private final Map<Long, String> tagNameCache = new ConcurrentHashMap<>();
    private final ExecutorService matchingExecutor = Executors.newCachedThreadPool();
    private final long matchingTimeoutMs = Long.parseLong(System.getenv().getOrDefault("MATCHING_API_TIMEOUT_MS", "3000"));

    private static final Set<String> VALID_ROLE_NAMES = Set.of("STUDENT", "PROJECT_CREATOR", "TEAM_LEADER", "MENTOR", "PLATFORM_ADMIN", "ADMIN");
    private static final Set<String> VALID_PROJECT_STATUS = Set.of("DRAFT", "RECRUITING", "IN_PROGRESS", "COMPLETED", "CANCELLED");
    private static final Set<String> VALID_CREDIT_LEVEL = Set.of("NEWBIE", "RELIABLE", "EXCELLENT", "OUTSTANDING");

    @Override
    public List<MatchResult> matchCandidates(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        Map<String, Object> projectData = buildProjectData(project);
        List<Map<String, Object>> candidates = getCandidatesForProject(project);

        // 硬性技能门槛：仅当项目存在必需技能时生效
        Object reqObj = projectData.get("skill_requirements");
        final boolean hasRequiredSkills = (reqObj instanceof List<?>) && ((List<?>) reqObj).stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(req -> Boolean.TRUE.equals(req.get("required")));
        final double minSkillThreshold = Double.parseDouble(System.getenv().getOrDefault("MATCHING_MIN_SKILL_SCORE", "0.2"));
        
        log.info("=== 匹配诊断 ===");
        log.info("项目ID: {}, 项目名称: {}", projectId, project.getTitle());
        log.info("召回候选人数量: {}", candidates.size());
        if (!candidates.isEmpty()) {
            candidates.forEach(c -> {
                Object userInfoObj = c.get("user");
                if (userInfoObj instanceof Map<?, ?> userInfo) {
                    log.info("  - 候选人: userId={}, username={}", 
                        userInfo.get("id"), userInfo.get("username"));
                } else {
                    log.info("  - 候选人: 数据结构异常");
                }
            });
        } else {
            log.warn("警告：没有召回任何候选人！");
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        MatchRequest request = new MatchRequest();
        request.setProjectId(projectId);
        request.setProject(projectData);
        request.setCandidates(candidates);

        try {
            List<MatchResult> results = withTimeout(() -> matchingClient.calculateMatch(request), Collections.emptyList());
            log.info("匹配服务返回结果数量: {}", results.size());
            if (!results.isEmpty()) {
                results.forEach(r -> log.info("  - 匹配结果: userId={}, score={}", 
                    r.getUserId(), r.getScore()));
            }
            
            return results.stream()
                    .filter(result -> {
                        if (!hasRequiredSkills) {
                            return true;
                        }
                        Map<String, Double> breakdown = result.getBreakdown();
                        if (breakdown == null) {
                            return false;
                        }
                        double skillScore = breakdown.getOrDefault("skill", 0D);
                        return skillScore >= minSkillThreshold;
                    })
                    .map(result -> {
                        try {
                            User user = userMapper.selectById(result.getUserId());
                            if (user != null) {
                                result.setUsername(user.getUsername());
                                UserProfile profile = profileService.getProfileByUserId(user.getId());
                                if (profile != null) {
                                    result.setDepartment(profile.getDepartment());
                                    result.setMajor(profile.getMajor());
                                    result.setGrade(profile.getGrade());
                                    result.setBio(profile.getBio());
                                }
                            }
                            if (result.getBreakdown() != null) {
                                result.setMatchReason(generateMatchReason(result.getBreakdown()));
                            } else {
                                result.setMatchReason("综合多维度评估的推荐结果");
                            }
                        } catch (Exception e) {
                            log.error("补充用户信息失败，userId={}", result.getUserId(), e);
                        }
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("调用匹配服务失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<MatchResult> matchTeamCandidates(Long teamId, String keyword) {
        Team team = teamService.getTeamById(teamId);
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }

        Map<String, Object> teamData = buildTeamAsProjectData(team, keyword);
        List<Map<String, Object>> candidates = getCandidatesForTeam(team, keyword);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        TeamMatchRequest request = new TeamMatchRequest();
        request.setTeamId(teamId);
        request.setTeam(teamData);
        request.setCandidates(candidates);

        try {
            List<MatchResult> results = withTimeout(() -> matchingClient.matchTeamToUsers(request), Collections.emptyList());
            log.info("团队找成员匹配结果数量: teamId={}, size={}", teamId, results.size());

            return results.stream().map(result -> {
                try {
                    User user = userMapper.selectById(result.getUserId());
                    if (user != null) {
                        result.setUsername(user.getUsername());
                        UserProfile profile = profileService.getProfileByUserId(user.getId());
                        if (profile != null) {
                            result.setDepartment(profile.getDepartment());
                            result.setMajor(profile.getMajor());
                            result.setGrade(profile.getGrade());
                            result.setBio(profile.getBio());
                        }
                    }
                    if (result.getBreakdown() != null && (result.getMatchReason() == null || result.getMatchReason().isBlank())) {
                        result.setMatchReason(generateMatchReason(result.getBreakdown()));
                    }
                } catch (Exception e) {
                    log.error("补充候选人信息失败，userId={}", result.getUserId(), e);
                }
                return result;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("团队找成员匹配失败, teamId={}", teamId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<ProjectWithMatchScore> matchProjectsForUser(Long userId, int page, int size) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            Map<String, Object> userData = buildUserData(user);

            LambdaQueryWrapper<Project> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(Project::getStatus, "RECRUITING")
                    .ne(Project::getCreatorId, userId)
                    .orderByDesc(Project::getCreatedAt);
            List<Project> projects = projectMapper.selectList(projectWrapper);
            if (projects.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> projectDataList = projects.stream()
                    .map(this::buildProjectData)
                    .collect(Collectors.toList());

            UserMatchRequest request = new UserMatchRequest();
            request.setUserId(userId);
            request.setUser(userData);
            request.setProjects(projectDataList);

            List<UserMatchResult> matchResults = withTimeout(() -> matchingClient.matchUserToProjects(request), Collections.emptyList());
            return convertToProjectWithMatchScore(matchResults, projects);
        } catch (Exception e) {
            log.error("匹配项目失败，用户ID={}", userId, e);
            throw new RuntimeException("匹配项目失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MatchResult> recommendTeammates(Long userId, int limit) {
        int safeLimit = Math.max(5, Math.min(limit, 50));
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            Map<String, Object> userData = buildUserData(user);

            // 召回候选用户：ACTIVE 且排除自己、导师/管理员/平台管理员
            int userScanLimit = Math.max(50, Integer.parseInt(System.getenv().getOrDefault("MATCHING_RECALL_USER_SCAN_LIMIT", "200")));

            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getStatus, "ACTIVE")
                    .ne(User::getId, userId)
                    .last("LIMIT " + userScanLimit);
            List<User> users = userMapper.selectList(userWrapper);
            if (users.isEmpty()) {
                return new ArrayList<>();
            }

            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            Map<Long, List<UserRole>> rolesByUserId = userRoleMapper.selectList(
                            new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds))
                    .stream().collect(Collectors.groupingBy(UserRole::getUserId));

            // 过滤掉导师、管理员和平台管理员
            users = users.stream().filter(u -> {
                List<UserRole> roles = rolesByUserId.getOrDefault(u.getId(), Collections.emptyList());
                return roles.stream().noneMatch(role ->
                        "MENTOR".equals(role.getRoleName()) ||
                                "ADMIN".equals(role.getRoleName()) ||
                                "PLATFORM_ADMIN".equals(role.getRoleName())
                );
            }).collect(Collectors.toList());

            if (users.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> candidateDataList = users.stream()
                    .map(this::buildUserData)
                    .collect(Collectors.toList());

            TeammateRecommendRequest request = new TeammateRecommendRequest();
            request.setUserId(userId);
            request.setUser(userData);
            request.setCandidates(candidateDataList);

            List<MatchResult> results = withTimeout(() -> matchingClient.recommendTeammates(request), Collections.emptyList());
            // 后续如果需要，可以在这里补充 matchReason/explain 字段
            return results.stream()
                    .limit(safeLimit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("智能组队推荐失败，用户ID={}", userId, e);
            throw new RuntimeException("智能组队推荐失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MatchResult> matchTaskAssignees(Long taskId, int limit) {
        int safeLimit = Math.max(3, Math.min(limit, 20));

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        Team team = teamService.getTeamById(task.getTeamId());
        if (team == null) {
            throw new RuntimeException("所属团队不存在");
        }

        // 获取团队成员作为候选集
        List<TeamMember> teamMembers = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, team.getId()));
        if (teamMembers.isEmpty()) {
            return Collections.emptyList();
        }

        // 排除已是该任务负责人的成员
        List<com.teamup.server.modules.team.dto.TaskAssigneeDTO> existingAssignees =
                taskAssigneeService.getAssigneesByTaskId(taskId);
        Set<Long> existingAssigneeUserIds = existingAssignees == null
                ? Collections.emptySet()
                : existingAssignees.stream().map(com.teamup.server.modules.team.dto.TaskAssigneeDTO::getUserId).collect(Collectors.toSet());

        List<Long> candidateUserIds = teamMembers.stream()
                .map(TeamMember::getUserId)
                .filter(uid -> !existingAssigneeUserIds.contains(uid))
                .collect(Collectors.toList());

        if (candidateUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userMapper.selectBatchIds(candidateUserIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> taskAsProject = buildTaskAsProjectData(task);
        List<Map<String, Object>> candidates = users.stream()
                .map(this::buildUserData)
                .collect(Collectors.toList());

        MatchRequest request = new MatchRequest();
        request.setProjectId(taskId);
        request.setProject(taskAsProject);
        request.setCandidates(candidates);

        List<MatchResult> results;
        try {
            results = withTimeout(() -> matchingClient.calculateMatch(request), Collections.emptyList());
        } catch (Exception e) {
            log.error("任务负责人匹配失败, taskId={}", taskId, e);
            return new ArrayList<>();
        }

        // 计算每个候选人在该团队中的当前任务负载（未完成任务数量），用于轻量惩罚
        Map<Long, Integer> userActiveTaskCount = new HashMap<>();
        for (Long uid : candidateUserIds) {
            List<Long> userTaskIds = taskAssigneeService.getTaskIdsByUserId(uid);
            if (userTaskIds == null || userTaskIds.isEmpty()) {
                userActiveTaskCount.put(uid, 0);
                continue;
            }
            List<Task> userTasks = taskMapper.selectBatchIds(userTaskIds);
            int activeCount = (int) userTasks.stream()
                    .filter(t -> Objects.equals(t.getTeamId(), team.getId()))
                    .filter(t -> !"DONE".equals(t.getStatus()))
                    .count();
            userActiveTaskCount.put(uid, activeCount);
        }

        // 按照匹配得分与任务负载进行调整排序
        List<MatchResult> adjusted = results.stream()
                .peek(r -> {
                    Long uid = r.getUserId();
                    if (uid == null) return;
                    int active = userActiveTaskCount.getOrDefault(uid, 0);
                    // 简单惩罚：每多一个未完成任务，整体分数降低 6%（最多 4 个）
                    double penaltyFactor = 1.0 - 0.06 * Math.min(active, 4);
                    if (penaltyFactor < 0.7) {
                        penaltyFactor = 0.7;
                    }
                    if (r.getScore() != null) {
                        r.setScore(r.getScore() * penaltyFactor);
                    }
                })
                .sorted((a, b) -> Double.compare(
                        b.getScore() != null ? b.getScore() : 0.0,
                        a.getScore() != null ? a.getScore() : 0.0))
                .limit(safeLimit)
                .collect(Collectors.toList());

        return adjusted;
    }

    private Map<String, Object> buildProjectData(Project project) {
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("id", project.getId());
        projectData.put("title", project.getTitle());
        projectData.put("description", project.getDescription());
        projectData.put("project_type", project.getProjectType());
        projectData.put("weekly_hours", project.getWeeklyHours());
        projectData.put("creator_id", project.getCreatorId());

        LambdaQueryWrapper<ProjectSkillRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectSkillRequirement::getProjectId, project.getId());
        List<ProjectSkillRequirement> requirements = skillRequirementMapper.selectList(wrapper);

        List<Map<String, Object>> skillReqs = requirements.stream().map(req -> {
            Map<String, Object> skill = new HashMap<>();
            skill.put("skill_name", req.getSkillName());
            skill.put("required", req.getRequired());
            skill.put("proficiency_level", req.getProficiencyLevel());
            return skill;
        }).collect(Collectors.toList());
        projectData.put("skill_requirements", skillReqs);

        // 查询并添加项目时间段需求
        LambdaQueryWrapper<ProjectTimeSlot> tsWrapper = new LambdaQueryWrapper<>();
        tsWrapper.eq(ProjectTimeSlot::getProjectId, project.getId());
        List<ProjectTimeSlot> timeSlots = projectTimeSlotMapper.selectList(tsWrapper);

        List<Map<String, Object>> timeSlotData = timeSlots.stream().map(ts -> {
            Map<String, Object> slot = new HashMap<>();
            slot.put("day_of_week", ts.getDayOfWeek());
            slot.put("start_time", ts.getStartTime().toString());
            slot.put("end_time", ts.getEndTime().toString());
            return slot;
        }).collect(Collectors.toList());
        projectData.put("time_slots", timeSlotData);

        return projectData;
    }

    /**
     * 将团队信息转换为匹配服务可识别的“项目”维度数据
     */
    private Map<String, Object> buildTeamAsProjectData(Team team, String keyword) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", team.getId());
        data.put("title", team.getTeamName());

        StringBuilder desc = new StringBuilder();
        if (team.getDescription() != null) {
            desc.append(team.getDescription());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (desc.length() > 0) {
                desc.append("；");
            }
            desc.append("团队当前重点招募方向：").append(keyword.trim());
        }
        data.put("description", desc.toString());

        // 在匹配服务中作为 project_type 使用，便于权重分组与AB实验
        data.put("project_type", "LONG_TERM_TEAM");

        // 长期团队预期每周投入时长（用于时间匹配兜底逻辑）
        data.put("weekly_hours", 8);

        data.put("creator_id", team.getLeaderId());

        // 将关键词映射为技能需求（通过 tags 表中技能标签做弱绑定）
        List<Map<String, Object>> skillReqs = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(Tag::getCategory, "SKILL").like(Tag::getName, kw);
            List<Tag> skillTags = tagMapper.selectList(tagWrapper);
            for (Tag tag : skillTags) {
                Map<String, Object> skill = new HashMap<>();
                skill.put("skill_name", tag.getName());
                skill.put("required", true);
                // 使用中等熟练度作为默认要求，具体分数由匹配服务根据用户熟练度细化
                skill.put("proficiency_level", "INTERMEDIATE");
                skillReqs.add(skill);
            }
        }
        data.put("skill_requirements", skillReqs);

        // 团队暂未维护结构化时间段，交由匹配服务使用 weekly_hours + 候选人 availability 兜底
        data.put("time_slots", new ArrayList<>());

        return data;
    }

    /**
     * 将任务信息转换为匹配服务可识别的“项目”维度数据（用于任务负责人匹配）
     */
    private Map<String, Object> buildTaskAsProjectData(Task task) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", task.getId());
        data.put("title", task.getTitle());
        data.put("description", task.getDescription());

        // 在匹配服务中作为 project_type 使用，便于分组调权
        data.put("project_type", "TASK");

        // 单个任务默认每周预计耗时 2 小时，用于时间匹配兜底
        data.put("weekly_hours", 2);

        data.put("creator_id", task.getCreatedBy());

        // 基于任务标题与描述弱匹配技能标签，构造 skill_requirements
        String text = String.join(" ",
                Optional.ofNullable(task.getTitle()).orElse(""),
                Optional.ofNullable(task.getDescription()).orElse("")
        ).toLowerCase();

        List<Map<String, Object>> skillReqs = new ArrayList<>();
        if (!text.isBlank()) {
            LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(Tag::getCategory, "SKILL");
            List<Tag> skillTags = tagMapper.selectList(tagWrapper);
            for (Tag tag : skillTags) {
                if (tag.getName() == null || tag.getName().isBlank()) {
                    continue;
                }
                String name = tag.getName().toLowerCase();
                if (text.contains(name)) {
                    Map<String, Object> skill = new HashMap<>();
                    skill.put("skill_name", tag.getName());
                    skill.put("required", true);
                    skill.put("proficiency_level", "INTERMEDIATE");
                    skillReqs.add(skill);
                }
            }
        }
        data.put("skill_requirements", skillReqs);

        // 任务暂不维护结构化时间段，交由匹配服务使用 weekly_hours + 候选人 availability 兜底
        data.put("time_slots", new ArrayList<>());

        return data;
    }

    private Map<String, Object> buildUserData(User user) {
        Map<String, Object> userData = new HashMap<>();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());

        UserProfile profile = profileService.getProfileByUserId(user.getId());
        if (profile != null) {
            userInfo.put("bio", profile.getBio());
            userInfo.put("project_experience", profile.getProjectExperience());
            userInfo.put("department", profile.getDepartment());
            userInfo.put("major", profile.getMajor());
            userInfo.put("grade", profile.getGrade());
        }

        UserCredit credit = profileService.getUserCredit(user.getId());
        userInfo.put("reputation_score", credit != null ? credit.getTotalCredit() : 0);

        userInfo.put("mentor_rating", getMentorRating(user.getId()));
        userInfo.put("interests", getUserInterests(user.getId()));
        userInfo.put("evaluations", getEvaluationStats(user.getId()));
        userInfo.put("mentor_member_evaluations", getMentorMemberEvaluationStats(user.getId()));
        userInfo.put("application_stats", getApplicationStats(user.getId()));
        userData.put("user", userInfo);

        userData.put("skills", getUserSkillsWithCertification(user.getId()));
        userData.put("availability", getUserAvailability(user.getId()));
        
        // 添加经验分数（系统验证的项目履历）
        try {
            com.teamup.server.modules.user.dto.ExperienceScore expScore = 
                projectHistoryService.calculateExperienceScore(user.getId());
            Map<String, Object> expScoreMap = new HashMap<>();
            expScoreMap.put("totalScore", expScore.getTotalScore());
            expScoreMap.put("isVerified", expScore.getIsVerified());
            expScoreMap.put("completedProjects", expScore.getCompletedProjects());
            expScoreMap.put("breakdown", expScore.getBreakdown());
            userData.put("experience_score", expScoreMap);
        } catch (Exception e) {
            log.warn("获取用户经验分数失败: userId={}", user.getId(), e);
            // 失败时不添加，Python 服务会降级到文本经验
        }

        Map<String, Object> creditMap = new HashMap<>();
        if (credit != null) {
            creditMap.put("credit_level", credit.getCreditLevel());
            creditMap.put("total_credit", credit.getTotalCredit());
        }
        userData.put("credit", creditMap);
        userData.put("collaboration_history", getCollaborationHistory(user.getId()));
        return userData;
    }

    private List<Map<String, Object>> getCandidatesForProject(Project project) {
        log.info("=== 开始为项目{}召回候选人 ===", project.getId());
        
        int userScanLimit = Math.max(50, Integer.parseInt(System.getenv().getOrDefault("MATCHING_RECALL_USER_SCAN_LIMIT", "200")));
        int maxRecallCandidates = Math.max(10, Integer.parseInt(System.getenv().getOrDefault("MATCHING_RECALL_MAX_CANDIDATES", "60")));

        LambdaQueryWrapper<ProjectSkillRequirement> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.eq(ProjectSkillRequirement::getProjectId, project.getId());
        List<ProjectSkillRequirement> requirements = skillRequirementMapper.selectList(reqWrapper);

        Set<String> requiredSkills = requirements.stream()
                .filter(req -> Boolean.TRUE.equals(req.getRequired()))
                .map(ProjectSkillRequirement::getSkillName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> allProjectSkills = requirements.stream()
                .map(ProjectSkillRequirement::getSkillName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        log.info("项目必需技能: {}", requiredSkills);
        log.info("项目所有技能: {}", allProjectSkills);

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getStatus, "ACTIVE")
                .ne(User::getId, project.getCreatorId())
                .last("LIMIT " + userScanLimit);
        List<User> users = userMapper.selectList(userWrapper);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        Map<Long, List<UserRole>> rolesByUserId = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(UserRole::getUserId));

        // 使用 user_tags 和 tags 表获取用户技能
        Map<Long, List<UserTag>> userTagsByUserId = userTagMapper.selectList(
                        new LambdaQueryWrapper<UserTag>().in(UserTag::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(UserTag::getUserId));
        
        // 获取所有技能标签
        Set<Long> tagIds = userTagsByUserId.values().stream()
                .flatMap(List::stream)
                .map(UserTag::getTagId)
                .collect(Collectors.toSet());
        
        final Map<Long, Tag> tagsById;
        if (!tagIds.isEmpty()) {
            tagsById = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, tag -> tag));
        } else {
            tagsById = new HashMap<>();
        }
        
        // 构建用户技能映射（只包含技能类标签）
        Map<Long, List<UserSkillInfo>> skillsByUserId = new HashMap<>();
        for (Map.Entry<Long, List<UserTag>> entry : userTagsByUserId.entrySet()) {
            Long userId = entry.getKey();
            List<UserSkillInfo> skills = entry.getValue().stream()
                    .filter(ut -> {
                        Tag tag = tagsById.get(ut.getTagId());
                        return tag != null && "SKILL".equals(tag.getCategory());
                    })
                    .map(ut -> {
                        Tag tag = tagsById.get(ut.getTagId());
                        UserSkillInfo skill = new UserSkillInfo();
                        skill.setSkillName(tag.getName());
                        skill.setProficiencyLevel(ut.getProficiencyLevel());
                        return skill;
                    })
                    .collect(Collectors.toList());
            skillsByUserId.put(userId, skills);
        }

        Map<Long, List<CollaborationHistory>> historiesByUserId = collaborationHistoryMapper.selectList(
                        new LambdaQueryWrapper<CollaborationHistory>().in(CollaborationHistory::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(CollaborationHistory::getUserId));

        Map<Long, List<UserInterest>> interestsByUserId = userInterestMapper.selectList(
                        new LambdaQueryWrapper<UserInterest>().in(UserInterest::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(UserInterest::getUserId));

        // 过滤掉导师、管理员和平台管理员
        users = users.stream().filter(user -> {
            List<UserRole> roles = rolesByUserId.getOrDefault(user.getId(), Collections.emptyList());
            return roles.stream().noneMatch(role -> 
                "MENTOR".equals(role.getRoleName()) || 
                "ADMIN".equals(role.getRoleName()) ||
                "PLATFORM_ADMIN".equals(role.getRoleName())
            );
        }).collect(Collectors.toList());
        
        log.info("过滤后的候选用户数量: {}", users.size());

        String projectText = String.join(" ",
                Optional.ofNullable(project.getTitle()).orElse(""),
                Optional.ofNullable(project.getDescription()).orElse(""),
                Optional.ofNullable(project.getProjectType()).orElse("")
        ).toLowerCase();

        List<Map.Entry<User, Double>> rankedUsers = new ArrayList<>();
        for (User user : users) {
            List<UserSkillInfo> userSkills = skillsByUserId.getOrDefault(user.getId(), Collections.emptyList());
            Set<String> userSkillNames = userSkills.stream()
                    .map(UserSkillInfo::getSkillName)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            // 详细日志: 打印用户技能匹配情况
            if (log.isInfoEnabled() && user.getId() == 12L) {
                log.info("=== 用户12技能匹配详情 ===");
                log.info("用户技能: {}", userSkillNames);
                log.info("项目必需技能: {}", requiredSkills);
                log.info("项目所有技能: {}", allProjectSkills);
            }

            double requiredHitRatio = requiredSkills.isEmpty() ? 1.0 :
                    requiredSkills.stream().filter(userSkillNames::contains).count() / (double) requiredSkills.size();
            double skillHitRatio = allProjectSkills.isEmpty() ? requiredHitRatio :
                    allProjectSkills.stream().filter(userSkillNames::contains).count() / (double) allProjectSkills.size();
            double skillRecall = 0.7 * requiredHitRatio + 0.3 * skillHitRatio;

            // 详细日志: 打印匹配分数
            if (log.isInfoEnabled() && user.getId() == 12L) {
                log.info("必需技能命中率: {}", requiredHitRatio);
                log.info("所有技能命中率: {}", skillHitRatio);
                log.info("技能召回分数: {}", skillRecall);
            }

            List<CollaborationHistory> histories = historiesByUserId.getOrDefault(user.getId(), Collections.emptyList());
            double historyRecall = histories.stream()
                    .filter(h -> Objects.equals(h.getPartnerId(), project.getCreatorId()))
                    .mapToDouble(h -> h.getCollaborationScore() == null ? 0.0 : h.getCollaborationScore().doubleValue())
                    .max().orElse(0.0);

            List<UserInterest> interests = interestsByUserId.getOrDefault(user.getId(), Collections.emptyList());
            double interestRecall = 0.0;
            if (!interests.isEmpty()) {
                long matched = interests.stream()
                        .map(UserInterest::getInterestName)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .filter(projectText::contains)
                        .count();
                interestRecall = matched / (double) interests.size();
            }

            double blended = 0.6 * skillRecall + 0.25 * historyRecall + 0.15 * interestRecall;
            if (!requiredSkills.isEmpty() && requiredHitRatio < 1.0) {
                double oldBlended = blended;
                blended *= (0.7 + 0.3 * requiredHitRatio);
                if (log.isInfoEnabled() && user.getId() == 12L) {
                    log.info("必需技能未满足,分数惩罚: {} -> {}", oldBlended, blended);
                }
            }
            
            if (log.isInfoEnabled() && user.getId() == 12L) {
                log.info("最终召回分数: {}", blended);
            }
            
            rankedUsers.add(new AbstractMap.SimpleEntry<>(user, blended));
        }

        // 过滤掉召回分数过低的用户(阈值可配置)
        double minRecallScore = Double.parseDouble(System.getenv().getOrDefault("MATCHING_MIN_RECALL_SCORE", "0.1"));
        rankedUsers = rankedUsers.stream()
                .filter(entry -> entry.getValue() >= minRecallScore)
                .collect(Collectors.toList());
        
        log.info("应用最低分数阈值({})后的候选人数量: {}", minRecallScore, rankedUsers.size());

        rankedUsers.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 日志: 显示排序后的前10名用户
        if (log.isInfoEnabled()) {
            log.info("=== 召回排序结果(前10名) ===");
            rankedUsers.stream().limit(10).forEach(entry -> 
                log.info("用户ID: {}, 用户名: {}, 分数: {}", 
                    entry.getKey().getId(), 
                    entry.getKey().getUsername(), 
                    entry.getValue())
            );
        }
        
        List<Map<String, Object>> result = rankedUsers.stream()
                .limit(maxRecallCandidates)
                .map(Map.Entry::getKey)
                .map(user -> {
                    Map<String, Object> candidateData = buildUserData(user);
                    candidateData.put("is_newbie", isNewbie(user));
                    return candidateData;
                })
                .collect(Collectors.toList());
        
        log.info("最终召回候选人数量: {}", result.size());
        return result;
    }

    /**
     * 团队找成员的候选人召回逻辑：
     * - 仅召回 ACTIVE 用户
     * - 排除本团队成员
     * - 排除导师 / 管理员 / 平台管理员
     * - 使用技能标签与兴趣与团队文本（含关键词）做一次轻量级召回排序与阈值过滤
     */
    private List<Map<String, Object>> getCandidatesForTeam(Team team, String keyword) {
        log.info("=== 开始为团队{}召回候选人 ===", team.getId());

        int userScanLimit = Math.max(50, Integer.parseInt(System.getenv().getOrDefault("MATCHING_RECALL_USER_SCAN_LIMIT", "200")));
        int maxRecallCandidates = Math.max(10, Integer.parseInt(System.getenv().getOrDefault("MATCHING_RECALL_MAX_CANDIDATES", "60")));

        // 根据关键词在技能标签中做一次弱匹配，构造“团队技能需求”集合
        Set<String> requiredSkills = new HashSet<>();
        Set<String> allTeamSkills = new HashSet<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(Tag::getCategory, "SKILL").like(Tag::getName, kw);
            List<Tag> skillTags = tagMapper.selectList(tagWrapper);
            for (Tag tag : skillTags) {
                String name = tag.getName();
                if (name != null && !name.isBlank()) {
                    String norm = name.toLowerCase();
                    requiredSkills.add(norm);
                    allTeamSkills.add(norm);
                }
            }
        }

        log.info("团队{} 关键词: {}, 解析得到技能需求: {}", team.getId(), keyword, requiredSkills);

        // 获取团队现有成员，避免重复推荐
        List<Long> memberIds = teamMemberMapper.selectList(
                        new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, team.getId()))
                .stream()
                .map(TeamMember::getUserId)
                .collect(Collectors.toList());

        // 初步扫描候选用户集
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getStatus, "ACTIVE")
                .notIn(!memberIds.isEmpty(), User::getId, memberIds)
                .last("LIMIT " + userScanLimit);
        List<User> users = userMapper.selectList(userWrapper);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        Map<Long, List<UserRole>> rolesByUserId = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(UserRole::getUserId));

        // 使用 user_tags 和 tags 表获取用户技能
        Map<Long, List<UserTag>> userTagsByUserId = userTagMapper.selectList(
                        new LambdaQueryWrapper<UserTag>().in(UserTag::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(UserTag::getUserId));

        Set<Long> tagIds = userTagsByUserId.values().stream()
                .flatMap(List::stream)
                .map(UserTag::getTagId)
                .collect(Collectors.toSet());

        final Map<Long, Tag> tagsById;
        if (!tagIds.isEmpty()) {
            tagsById = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, tag -> tag));
        } else {
            tagsById = new HashMap<>();
        }

        // 构建用户技能映射（只包含技能类标签）
        Map<Long, List<UserSkillInfo>> skillsByUserId = new HashMap<>();
        for (Map.Entry<Long, List<UserTag>> entry : userTagsByUserId.entrySet()) {
            Long uid = entry.getKey();
            List<UserSkillInfo> skills = entry.getValue().stream()
                    .filter(ut -> {
                        Tag tag = tagsById.get(ut.getTagId());
                        return tag != null && "SKILL".equals(tag.getCategory());
                    })
                    .map(ut -> {
                        Tag tag = tagsById.get(ut.getTagId());
                        UserSkillInfo skill = new UserSkillInfo();
                        skill.setSkillName(tag.getName());
                        skill.setProficiencyLevel(ut.getProficiencyLevel());
                        return skill;
                    })
                    .collect(Collectors.toList());
            skillsByUserId.put(uid, skills);
        }

        Map<Long, List<CollaborationHistory>> historiesByUserId = collaborationHistoryMapper.selectList(
                        new LambdaQueryWrapper<CollaborationHistory>().in(CollaborationHistory::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(CollaborationHistory::getUserId));

        Map<Long, List<UserInterest>> interestsByUserId = userInterestMapper.selectList(
                        new LambdaQueryWrapper<UserInterest>().in(UserInterest::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(UserInterest::getUserId));

        // 过滤掉导师、管理员和平台管理员
        users = users.stream().filter(user -> {
            List<UserRole> roles = rolesByUserId.getOrDefault(user.getId(), Collections.emptyList());
            return roles.stream().noneMatch(role ->
                    "MENTOR".equals(role.getRoleName()) ||
                            "ADMIN".equals(role.getRoleName()) ||
                            "PLATFORM_ADMIN".equals(role.getRoleName())
            );
        }).collect(Collectors.toList());

        log.info("团队{} 过滤后的候选用户数量: {}", team.getId(), users.size());

        String teamText = String.join(" ",
                Optional.ofNullable(team.getTeamName()).orElse(""),
                Optional.ofNullable(team.getDescription()).orElse(""),
                Optional.ofNullable(keyword).orElse("")
        ).toLowerCase();

        List<Map.Entry<User, Double>> rankedUsers = new ArrayList<>();
        for (User user : users) {
            List<UserSkillInfo> userSkills = skillsByUserId.getOrDefault(user.getId(), Collections.emptyList());
            Set<String> userSkillNames = userSkills.stream()
                    .map(UserSkillInfo::getSkillName)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            double requiredHitRatio = requiredSkills.isEmpty() ? 1.0 :
                    requiredSkills.stream().filter(userSkillNames::contains).count() / (double) requiredSkills.size();
            double skillHitRatio = allTeamSkills.isEmpty() ? requiredHitRatio :
                    allTeamSkills.stream().filter(userSkillNames::contains).count() / (double) allTeamSkills.size();
            double skillRecall = 0.7 * requiredHitRatio + 0.3 * skillHitRatio;

            List<CollaborationHistory> histories = historiesByUserId.getOrDefault(user.getId(), Collections.emptyList());
            double historyRecall = histories.stream()
                    .filter(h -> Objects.equals(h.getPartnerId(), team.getLeaderId()))
                    .mapToDouble(h -> h.getCollaborationScore() == null ? 0.0 : h.getCollaborationScore().doubleValue())
                    .max().orElse(0.0);

            List<UserInterest> interests = interestsByUserId.getOrDefault(user.getId(), Collections.emptyList());
            double interestRecall = 0.0;
            if (!interests.isEmpty()) {
                long matched = interests.stream()
                        .map(UserInterest::getInterestName)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .filter(teamText::contains)
                        .count();
                interestRecall = matched / (double) interests.size();
            }

            double blended = 0.6 * skillRecall + 0.25 * historyRecall + 0.15 * interestRecall;

            // 如果存在关键词映射出的必需技能，但用户满足度较低，则做一次惩罚，避免误召回
            if (!requiredSkills.isEmpty() && requiredHitRatio < 1.0) {
                blended *= (0.7 + 0.3 * requiredHitRatio);
            }

            rankedUsers.add(new AbstractMap.SimpleEntry<>(user, blended));
        }

        double minRecallScore = Double.parseDouble(System.getenv().getOrDefault("MATCHING_MIN_RECALL_SCORE", "0.1"));
        rankedUsers = rankedUsers.stream()
                .filter(entry -> entry.getValue() >= minRecallScore)
                .collect(Collectors.toList());

        log.info("团队{} 应用最低分数阈值({})后的候选人数量: {}", team.getId(), minRecallScore, rankedUsers.size());

        rankedUsers.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Map<String, Object>> result = rankedUsers.stream()
                .limit(maxRecallCandidates)
                .map(Map.Entry::getKey)
                .map(user -> {
                    Map<String, Object> candidateData = buildUserData(user);
                    candidateData.put("is_newbie", isNewbie(user));
                    return candidateData;
                })
                .collect(Collectors.toList());

        log.info("团队{} 最终召回候选人数量: {}", team.getId(), result.size());
        return result;
    }

    private boolean isNewbie(User user) {
        long daysSinceRegistration = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
        return daysSinceRegistration < 30;
    }

    /**
     * 将技能熟练度等级转换为数值分数
     * 使用统一的 ProficiencyLevel 枚举进行转换
     */
    private double convertProficiencyToNumber(String level) {
        return com.teamup.server.common.enums.ProficiencyLevel.getScore(level);
    }

    private List<ProjectWithMatchScore> convertToProjectWithMatchScore(List<UserMatchResult> matchResults, List<Project> projects) {
        Map<Long, Project> projectMap = projects.stream().collect(Collectors.toMap(Project::getId, p -> p));
        return matchResults.stream().map(result -> {
            ProjectWithMatchScore dto = new ProjectWithMatchScore();
            Project project = projectMap.get(result.getProjectId());
            if (project != null) {
                dto.setProject(project);
                dto.setMatchScore(result.getMatchScore());
                dto.setBreakdown(result.getBreakdown());
                dto.setMatchReason(generateMatchReason(result.getBreakdown()));
                dto.setTimeExplanation(result.getTimeExplanation());
            }
            return dto;
        }).filter(dto -> dto.getProject() != null).collect(Collectors.toList());
    }

    private String generateMatchReason(Map<String, Double> breakdown) {
        List<String> reasons = new ArrayList<>();
        if (scoreOf(breakdown, "skill") > 0.7) reasons.add("技能高度匹配");
        if (scoreOf(breakdown, "time") > 0.7) reasons.add("时间安排合适");
        if (scoreOf(breakdown, "goal") > 0.6) reasons.add("目标方向一致");
        if (scoreOf(breakdown, "credit") > 0.8) reasons.add("信誉良好");
        return reasons.isEmpty() ? "综合评估匹配" : String.join("、", reasons);
    }

    private double scoreOf(Map<String, Double> breakdown, String key) {
        if (breakdown == null) return 0D;
        Double v = breakdown.get(key);
        return v == null ? 0D : v;
    }

    private List<Map<String, Object>> getUserSkillsWithCertification(Long userId) {
        LambdaQueryWrapper<UserTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(UserTag::getUserId, userId);
        List<UserTag> userTags = userTagMapper.selectList(tagWrapper);

        // 获取所有标签详情,过滤出技能类标签
        Set<Long> tagIds = userTags.stream().map(UserTag::getTagId).collect(Collectors.toSet());
        Map<Long, Tag> tagsMap = new HashMap<>();
        if (!tagIds.isEmpty()) {
            tagsMap = tagMapper.selectBatchIds(tagIds).stream()
                    .filter(tag -> "SKILL".equals(tag.getCategory()))
                    .collect(Collectors.toMap(Tag::getId, tag -> tag));
        }

        List<SkillCertification> certs = skillCertificationMapper.getApprovedCertifications(userId);
        Map<String, String> certMap = certs.stream().collect(Collectors.toMap(
                SkillCertification::getSkillName,
                SkillCertification::getCertificationType,
                (a, b) -> a
        ));

        final Map<Long, Tag> finalTagsMap = tagsMap;
        return userTags.stream()
                .filter(userTag -> finalTagsMap.containsKey(userTag.getTagId()))
                .map(userTag -> {
                    Tag tag = finalTagsMap.get(userTag.getTagId());
                    Map<String, Object> skill = new HashMap<>();
                    skill.put("skill_name", tag.getName());
                    skill.put("proficiency_level", convertProficiencyToNumber(userTag.getProficiencyLevel()));
                    skill.put("certification_type", certMap.getOrDefault(tag.getName(), "SELF_CLAIM"));
                    return skill;
                }).collect(Collectors.toList());
    }

    private List<String> getUserInterests(Long userId) {
        LambdaQueryWrapper<UserInterest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInterest::getUserId, userId);
        return userInterestMapper.selectList(wrapper).stream()
                .map(UserInterest::getInterestName)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getUserAvailability(Long userId) {
        LambdaQueryWrapper<UserAvailability> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAvailability::getUserId, userId);
        return userAvailabilityMapper.selectList(wrapper).stream().map(a -> {
            Map<String, Object> av = new HashMap<>();
            av.put("day_of_week", a.getDayOfWeek());
            av.put("start_time", a.getStartTime().toString());
            av.put("end_time", a.getEndTime().toString());
            return av;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getCollaborationHistory(Long userId) {
        LambdaQueryWrapper<CollaborationHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollaborationHistory::getUserId, userId);
        return collaborationHistoryMapper.selectList(wrapper).stream().map(h -> {
            Map<String, Object> hist = new HashMap<>();
            hist.put("partner_id", h.getPartnerId());
            hist.put("project_id", h.getProjectId());
            hist.put("collaboration_score", h.getCollaborationScore());
            return hist;
        }).collect(Collectors.toList());
    }

    private double getMentorRating(Long userId) {
        try {
            LambdaQueryWrapper<TeamMember> mentorWrapper = new LambdaQueryWrapper<>();
            mentorWrapper.eq(TeamMember::getUserId, userId).eq(TeamMember::getRole, "MENTOR");
            TeamMember mentorRelation = teamMemberMapper.selectOne(mentorWrapper);
            if (mentorRelation == null || mentorRelation.getUserId() == null) return 0.0;

            com.teamup.server.modules.mentor.entity.MentorPerformance performance = performanceMapper.selectOne(
                    new LambdaQueryWrapper<com.teamup.server.modules.mentor.entity.MentorPerformance>()
                            .eq(com.teamup.server.modules.mentor.entity.MentorPerformance::getMentorId, mentorRelation.getUserId())
            );
            return performance != null && performance.getRating() != null ? performance.getRating().doubleValue() : 0.0;
        } catch (Exception e) {
            log.warn("获取用户导师评分失败，userId={}", userId, e);
            return 0.0;
        }
    }

    private Map<String, Object> getEvaluationStats(Long userId) {
        try {
            Map<String, Double> avgScores = evaluationMapper.getAverageScores(userId);
            Map<String, Object> stats = new HashMap<>();
            
            // 获取平均分
            double avgTech = avgScores.getOrDefault("avgTech", 0.0);
            double avgCollab = avgScores.getOrDefault("avgCollab", 0.0);
            double avgTask = avgScores.getOrDefault("avgTask", 0.0);
            
            stats.put("avg_tech_contribution", avgTech);
            stats.put("avg_collaboration", avgCollab);
            stats.put("avg_task_completion", avgTask);
            
            // 添加评价数据质量指标：评价数量（用于匹配服务判断数据充足性）
            // 注意：这里不直接查询数量，避免额外查询，匹配服务可以根据平均值是否为0判断
            // 如果所有平均值都是0，说明没有评价数据
            boolean hasData = avgTech > 0 || avgCollab > 0 || avgTask > 0;
            stats.put("has_evaluation_data", hasData);
            
            // 计算综合平均分（用于快速判断）
            double overallAvg = (avgTech + avgCollab + avgTask) / 3.0;
            stats.put("overall_avg_score", overallAvg);
            
            return stats;
        } catch (Exception e) {
            log.warn("获取用户评价统计失败: userId={}", userId, e);
            Map<String, Object> stats = new HashMap<>();
            stats.put("avg_tech_contribution", 0.0);
            stats.put("avg_collaboration", 0.0);
            stats.put("avg_task_completion", 0.0);
            stats.put("has_evaluation_data", false);
            stats.put("overall_avg_score", 0.0);
            return stats;
        }
    }

    /**
     * 获取成员的导师评价聚合数据（用于匹配协作维度的补充信号）
     * 注意：只在存在导师评价数据时才会对匹配产生正向影响，避免无导师团队成员吃亏。
     */
    private Map<String, Object> getMentorMemberEvaluationStats(Long userId) {
        try {
            Map<String, Object> raw = mentorMemberEvaluationMapper.getAverageStatsByMemberId(userId);
            Map<String, Object> stats = new HashMap<>();

            long evalCount = 0L;
            if (raw != null && raw.get("evalCount") instanceof Number) {
                evalCount = ((Number) raw.get("evalCount")).longValue();
            }

            double avgScore = raw != null && raw.get("avgScore") instanceof Number
                    ? ((Number) raw.get("avgScore")).doubleValue()
                    : 0.0;
            double avgTech = raw != null && raw.get("avgTech") instanceof Number
                    ? ((Number) raw.get("avgTech")).doubleValue()
                    : 0.0;
            double avgCollab = raw != null && raw.get("avgCollab") instanceof Number
                    ? ((Number) raw.get("avgCollab")).doubleValue()
                    : 0.0;
            double avgLearning = raw != null && raw.get("avgLearning") instanceof Number
                    ? ((Number) raw.get("avgLearning")).doubleValue()
                    : 0.0;
            double avgTask = raw != null && raw.get("avgTask") instanceof Number
                    ? ((Number) raw.get("avgTask")).doubleValue()
                    : 0.0;

            stats.put("has_mentor_member_evaluation", evalCount > 0);
            stats.put("evaluation_count", evalCount);
            // 原始均值：score 0-100；其余 1-5
            stats.put("avg_score", avgScore);
            stats.put("avg_technical_ability", avgTech);
            stats.put("avg_collaboration", avgCollab);
            stats.put("avg_learning_attitude", avgLearning);
            stats.put("avg_task_completion", avgTask);
            return stats;
        } catch (Exception e) {
            log.debug("获取导师成员评价统计失败: userId={}", userId, e);
            Map<String, Object> stats = new HashMap<>();
            stats.put("has_mentor_member_evaluation", false);
            stats.put("evaluation_count", 0);
            stats.put("avg_score", 0.0);
            stats.put("avg_technical_ability", 0.0);
            stats.put("avg_collaboration", 0.0);
            stats.put("avg_learning_attitude", 0.0);
            stats.put("avg_task_completion", 0.0);
            return stats;
        }
    }

    private Map<String, Object> getApplicationStats(Long userId) {
        try {
            LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProjectApplication::getApplicantId, userId);
            List<ProjectApplication> applications = projectApplicationMapper.selectList(wrapper);
            long total = applications.size();
            long approved = applications.stream().filter(app -> "APPROVED".equals(app.getStatus())).count();
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("approved", approved);
            return stats;
        } catch (Exception e) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", 0);
            stats.put("approved", 0);
            return stats;
        }
    }

    private <T> T withTimeout(Callable<T> task, T fallback) {
        Future<T> future = matchingExecutor.submit(task);
        try {
            return future.get(matchingTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("匹配接口调用超时，触发降级返回，timeoutMs={}", matchingTimeoutMs);
            return fallback;
        } catch (Exception e) {
            log.warn("匹配接口调用失败，触发降级返回", e);
            return fallback;
        }
    }

    private String resolveTagName(Long tagId) {
        if (tagId == null) {
            return null;
        }
        return tagNameCache.computeIfAbsent(tagId, id -> {
            com.teamup.server.modules.tag.entity.Tag tag = tagMapper.selectById(id);
            return tag == null ? "" : Optional.ofNullable(tag.getName()).orElse("");
        });
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void inspectMatchingDataIntegrityDaily() {
        try {
            long invalidRoleCount = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()).stream()
                    .map(UserRole::getRoleName)
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase)
                    .filter(role -> !VALID_ROLE_NAMES.contains(role))
                    .count();

            long invalidProjectStatusCount = projectMapper.selectList(new LambdaQueryWrapper<Project>()).stream()
                    .map(Project::getStatus)
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase)
                    .filter(status -> !VALID_PROJECT_STATUS.contains(status))
                    .count();

            long invalidCreditLevelCount = userCreditMapper.selectList(new LambdaQueryWrapper<UserCredit>()).stream()
                    .map(UserCredit::getCreditLevel)
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase)
                    .filter(level -> !VALID_CREDIT_LEVEL.contains(level))
                    .count();

            log.info("[MatchingDataIntegrity] invalidRoles={}, invalidProjectStatus={}, invalidCreditLevel={}, tagCacheSize={}",
                    invalidRoleCount, invalidProjectStatusCount, invalidCreditLevelCount, tagNameCache.size());
        } catch (Exception e) {
            log.warn("匹配数据完整性巡检任务执行失败", e);
        }
    }

    @Override
    public void reportMatchFeedback(Long projectId, Long userId, String projectType, String event, String source, Long eventTime, Map<String, Double> breakdown) {
        try {
            MatchFeedbackRequest request = new MatchFeedbackRequest();
            request.setProjectId(projectId);
            request.setUserId(userId);
            request.setProjectType(projectType);
            request.setEvent(event);
            request.setSource(source);
            request.setEventTime(eventTime);
            request.setBreakdown(breakdown);
            matchingClient.sendMatchFeedback(request);
        } catch (Exception e) {
            log.warn("匹配反馈回流失败: projectId={}, userId={}, event={}", projectId, userId, event, e);
        }
    }

    /**
     * 用户技能信息DTO
     */
    @lombok.Data
    private static class UserSkillInfo {
        private String skillName;
        private String proficiencyLevel;
    }


    @Override
    public List<MatchResult> recommendTeammatesForProject(Long userId, String projectTitle, String projectType,
                                                          List<Map<String, Object>> skillRequirements,
                                                          List<Map<String, Object>> timeSlots,
                                                          Integer weeklyHours, Integer expectedDuration) {
        try {
            User currentUser = userMapper.selectById(userId);
            if (currentUser == null) {
                throw new RuntimeException("用户不存在");
            }

            // 构建虚拟项目数据（用于匹配，但不保存到数据库）
            Map<String, Object> projectData = new HashMap<>();
            projectData.put("title", projectTitle);
            // matching-service 侧使用 snake_case 字段
            projectData.put("project_type", projectType);
            projectData.put("weekly_hours", weeklyHours);
            projectData.put("expected_duration", expectedDuration);

            // 处理技能需求
            if (skillRequirements != null && !skillRequirements.isEmpty()) {
                List<Map<String, Object>> skills = new ArrayList<>();
                for (Map<String, Object> req : skillRequirements) {
                    Map<String, Object> skill = new HashMap<>();
                    Long tagId = null;
                    if (req.get("tagId") instanceof Integer) {
                        tagId = ((Integer) req.get("tagId")).longValue();
                    } else if (req.get("tagId") instanceof Long) {
                        tagId = (Long) req.get("tagId");
                    }

                    if (tagId != null) {
                        // matching-service 侧读取: skill_name / required / proficiency_level
                        skill.put("skill_name", resolveTagName(tagId));
                        skill.put("required", req.get("required"));
                        skill.put("proficiency_level", req.get("proficiencyLevel"));
                        skills.add(skill);
                    }
                }
                projectData.put("skill_requirements", skills);
            } else {
                projectData.put("skill_requirements", new ArrayList<>());
            }

            // 处理时间段
            if (timeSlots != null && !timeSlots.isEmpty()) {
                // 前端传 dayOfWeek/startTime/endTime；matching-service 侧读取 day_of_week/start_time/end_time
                List<Map<String, Object>> normalizedSlots = new ArrayList<>();
                for (Map<String, Object> slot : timeSlots) {
                    if (slot == null) continue;
                    Map<String, Object> s = new HashMap<>();
                    Object day = slot.get("dayOfWeek");
                    Object start = slot.get("startTime");
                    Object end = slot.get("endTime");
                    // 兼容：如果已经是 snake_case，也接受
                    if (day == null) day = slot.get("day_of_week");
                    if (start == null) start = slot.get("start_time");
                    if (end == null) end = slot.get("end_time");
                    s.put("day_of_week", day);
                    s.put("start_time", start);
                    s.put("end_time", end);
                    normalizedSlots.add(s);
                }
                projectData.put("time_slots", normalizedSlots);
            } else {
                projectData.put("time_slots", new ArrayList<>());
            }

            // 召回候选用户：ACTIVE 且排除自己、导师/管理员/平台管理员
            int userScanLimit = Math.max(50, Integer.parseInt(System.getenv().getOrDefault("MATCHING_RECALL_USER_SCAN_LIMIT", "200")));

            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getStatus, "ACTIVE")
                    .ne(User::getId, userId)
                    .last("LIMIT " + userScanLimit);
            List<User> users = userMapper.selectList(userWrapper);
            if (users.isEmpty()) {
                return new ArrayList<>();
            }

            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            Map<Long, List<UserRole>> rolesByUserId = userRoleMapper.selectList(
                            new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds))
                    .stream().collect(Collectors.groupingBy(UserRole::getUserId));

            // 过滤掉导师、管理员和平台管理员
            users = users.stream().filter(u -> {
                List<UserRole> roles = rolesByUserId.getOrDefault(u.getId(), Collections.emptyList());
                return roles.stream().noneMatch(role ->
                        "MENTOR".equals(role.getRoleName()) ||
                                "ADMIN".equals(role.getRoleName()) ||
                                "PLATFORM_ADMIN".equals(role.getRoleName())
                );
            }).collect(Collectors.toList());

            if (users.isEmpty()) {
                return new ArrayList<>();
            }

            // 候选人ID集合：用于约束匹配服务返回结果必须来自本次召回候选人，避免出现“幽灵 userId”
            Set<Long> candidateIdSet = users.stream()
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<Map<String, Object>> candidateDataList = users.stream()
                    .map(this::buildUserData)
                    .collect(Collectors.toList());

            // 调用匹配服务
            MatchRequest matchRequest = new MatchRequest();
            matchRequest.setProjectId(-1L); // 虚拟项目ID
            matchRequest.setProject(projectData);
            matchRequest.setCandidates(candidateDataList);

            List<MatchResult> results = withTimeout(() -> matchingClient.calculateMatch(matchRequest), Collections.emptyList());

            // 防御：只保留本次候选人集合中的 userId，避免前端出现“未设置昵称/用户+数字”的兜底展示
            if (!results.isEmpty()) {
                int before = results.size();
                results = results.stream()
                        .filter(r -> r != null && r.getUserId() != null && candidateIdSet.contains(r.getUserId()))
                        .collect(Collectors.toList());
                int after = results.size();
                if (after < before) {
                    log.warn("匹配服务返回了不在候选人集合中的 userId，已过滤: before={}, after={}, projectTitle={}",
                            before, after, projectTitle);
                }
            }

            // 补充用户详细信息
            if (!results.isEmpty()) {
                Set<Long> resultUserIds = results.stream()
                        .map(MatchResult::getUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                
                if (!resultUserIds.isEmpty()) {
                    Map<Long, User> userMap = userMapper.selectBatchIds(resultUserIds).stream()
                            .collect(Collectors.toMap(User::getId, u -> u));
                    
                    Map<Long, UserProfile> profileMap = new HashMap<>();
                    for (Long uid : resultUserIds) {
                        UserProfile profile = profileService.getProfileByUserId(uid);
                        if (profile != null) {
                            profileMap.put(uid, profile);
                        }
                    }
                    
                    // 填充用户信息
                    for (MatchResult result : results) {
                        User user = userMap.get(result.getUserId());
                        if (user != null) {
                            // 前端展示名优先使用 nickname，其次 username
                            String displayName = user.getNickname();
                            if (displayName == null || displayName.isBlank()) {
                                displayName = user.getUsername();
                            }
                            result.setUsername(displayName);
                            
                            UserProfile profile = profileMap.get(user.getId());
                            if (profile != null) {
                                result.setDepartment(profile.getDepartment());
                                result.setMajor(profile.getMajor());
                                result.setGrade(profile.getGrade());
                                result.setBio(profile.getBio());
                            }
                        } else {
                            // 防御：匹配服务返回了本库不存在的 userId，会导致前端展示“用户{id}”兜底
                            log.warn("匹配结果中存在未知用户ID，无法补充用户信息: userId={}", result.getUserId());
                        }
                    }

                    // 强约束：只返回能补全出展示名的“真实用户”
                    int beforeNameFilter = results.size();
                    results = results.stream()
                            .filter(r -> r != null && r.getUserId() != null
                                    && r.getUsername() != null && !r.getUsername().isBlank())
                            .collect(Collectors.toList());
                    int afterNameFilter = results.size();
                    if (afterNameFilter < beforeNameFilter) {
                        log.warn("匹配结果中存在无法补全用户名的用户，已过滤: before={}, after={}, projectTitle={}",
                                beforeNameFilter, afterNameFilter, projectTitle);
                    }
                }
            }

            // 限制返回数量为20
            return results.stream()
                    .limit(20)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("为项目推荐队友失败，用户ID={}", userId, e);
            throw new RuntimeException("为项目推荐队友失败: " + e.getMessage(), e);
        }
    }

}
