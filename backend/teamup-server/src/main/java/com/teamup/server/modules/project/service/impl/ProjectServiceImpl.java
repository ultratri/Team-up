package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.api.ApiErrorCode;
import com.teamup.server.common.enums.ProjectStatus;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectApplication;
import com.teamup.server.modules.project.entity.ProjectSkillRequirement;
import com.teamup.server.modules.project.entity.ProjectTimeSlot;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.project.mapper.ProjectApplicationMapper;
import com.teamup.server.modules.project.mapper.ProjectRecommendationMapper;
import com.teamup.server.modules.project.mapper.ProjectSkillRequirementMapper;
import com.teamup.server.modules.project.mapper.ProjectTimeSlotMapper;
import com.teamup.server.modules.project.service.ProjectService;
import com.teamup.server.modules.tag.entity.Tag;
import com.teamup.server.modules.tag.mapper.TagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目服务实现
 */
@Service
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectApplicationMapper applicationMapper;
    private final ProjectRecommendationMapper recommendationMapper;
    private final ProjectSkillRequirementMapper skillRequirementMapper;
    private final ProjectTimeSlotMapper timeSlotMapper;
    private final TagMapper tagMapper;
    private final StringRedisTemplate redisTemplate;
    private final com.teamup.server.modules.notification.service.NotificationService notificationService;
    private final com.teamup.server.modules.user.service.UserService userService;
    private final com.teamup.server.modules.user.service.CreditService creditService;
    private final com.teamup.server.modules.team.service.TeamService teamService;
    private final com.teamup.server.modules.team.service.TeamProjectService teamProjectService;
    private final com.teamup.server.modules.user.service.ProjectHistoryService projectHistoryService;
    
    public ProjectServiceImpl(ProjectMapper projectMapper,
                              ProjectApplicationMapper applicationMapper,
                              ProjectRecommendationMapper recommendationMapper,
                              ProjectSkillRequirementMapper skillRequirementMapper,
                              ProjectTimeSlotMapper timeSlotMapper,
                              TagMapper tagMapper,
                              StringRedisTemplate redisTemplate,
                              com.teamup.server.modules.notification.service.NotificationService notificationService,
                              com.teamup.server.modules.user.service.UserService userService,
                              com.teamup.server.modules.user.service.CreditService creditService,
                              @org.springframework.context.annotation.Lazy com.teamup.server.modules.team.service.TeamService teamService,
                              com.teamup.server.modules.team.service.TeamProjectService teamProjectService,
                              com.teamup.server.modules.user.service.ProjectHistoryService projectHistoryService) {
        this.projectMapper = projectMapper;
        this.applicationMapper = applicationMapper;
        this.recommendationMapper = recommendationMapper;
        this.skillRequirementMapper = skillRequirementMapper;
        this.timeSlotMapper = timeSlotMapper;
        this.tagMapper = tagMapper;
        this.redisTemplate = redisTemplate;
        this.notificationService = notificationService;
        this.userService = userService;
        this.creditService = creditService;
        this.teamService = teamService;
        this.teamProjectService = teamProjectService;
        this.projectHistoryService = projectHistoryService;
    }
    
    private static final String STATUS_PENDING = "PENDING";

    @Override
    public Page<Project> getProjectList(int page, int size, String type, String status, String keyword, Long userId) {
        // 走 MySQL 逻辑
        Page<Project> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        
        // 查询用户可见的项目ID（用户创建的 + 用户所在团队的项目）
        final List<Long> visibleProjectIds = (userId != null) 
            ? projectMapper.selectVisibleProjectIds(userId) 
            : null;
        
        // 如果没有指定状态，默认排除草稿状态（项目大厅场景）
        // 如果指定了状态，则按指定状态过滤（我的项目场景）
        if (StringUtils.hasText(status)) {
            wrapper.eq(Project::getStatus, status);
        } else {
            // 项目大厅：排除草稿状态，只显示公开的项目或用户可见的项目
            if (visibleProjectIds != null && !visibleProjectIds.isEmpty()) {
                // 显示：非草稿的公开项目 或 用户可见的项目（包括草稿）
                wrapper.and(w -> w.ne(Project::getStatus, ProjectStatus.DRAFT.name())
                                  .or()
                                  .in(Project::getId, visibleProjectIds));
            } else {
                // 未登录或无可见项目：只显示非草稿的公开项目
                wrapper.ne(Project::getStatus, ProjectStatus.DRAFT.name());
            }
        }
        
        // 类型筛选
        if (StringUtils.hasText(type)) {
            wrapper.eq(Project::getProjectType, type);
        }
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Project::getTitle, keyword)
                             .or()
                             .like(Project::getDescription, keyword));
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Project::getCreatedAt);
        
        return projectMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<Project> getMyProjectList(int page, int size, String type, String status, String keyword, Long userId) {
        // 我的项目：只返回当前用户创建的项目
        Page<Project> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        
        // 只返回当前用户创建的项目
        if (userId != null) {
            wrapper.eq(Project::getCreatorId, userId);
        }
        
        // 类型筛选
        if (StringUtils.hasText(type)) {
            wrapper.eq(Project::getProjectType, type);
        }
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(Project::getStatus, status);
        }
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Project::getTitle, keyword)
                             .or()
                             .like(Project::getDescription, keyword));
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Project::getCreatedAt);
        
        return projectMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Project getProjectById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project != null) {
            // 获取创建者用户名
            try {
                com.teamup.server.modules.user.entity.User creator = userService.getUserById(project.getCreatorId());
                if (creator != null) {
                    project.setCreatorName(creator.getUsername());
                }
            } catch (Exception e) {
                log.error("获取项目创建者信息失败", e);
            }
        }
        return project;
    }
    
    /**
     * 增加项目浏览次数（带防刷机制）
     * 同一用户 24 小时内只计数一次
     */
    public void incrementProjectViews(Long projectId, Long userId, String ipAddress) {
        try {
            // 构建 Redis key：project_view:{projectId}:{userId或IP}
            String viewKey = "project_view:" + projectId + ":" + 
                           (userId != null ? "user_" + userId : "ip_" + ipAddress);
            
            // 检查是否已经浏览过（24小时内）
            Boolean hasViewed = redisTemplate.hasKey(viewKey);
            
            if (hasViewed == null || !hasViewed) {
                // 增加浏览次数
                Project project = projectMapper.selectById(projectId);
                if (project != null) {
                    project.setViews(project.getViews() + 1);
                    projectMapper.updateById(project);
                    
                    // 设置 Redis 标记，24 小时过期
                    redisTemplate.opsForValue().set(viewKey, "1", 24, java.util.concurrent.TimeUnit.HOURS);
                    
                    log.debug("项目 {} 浏览次数 +1，当前浏览次数：{}", projectId, project.getViews());
                }
            } else {
                log.debug("项目 {} 在 24 小时内已被该用户/IP 浏览过，不重复计数", projectId);
            }
        } catch (Exception e) {
            // 防刷机制失败不影响主流程
            log.error("增加项目浏览次数失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Project createProject(Project project, Long userId, List<Map<String, Object>> skillRequirements, List<Map<String, Object>> timeSlots) {
        project.setCreatorId(userId);
        project.setStatus(ProjectStatus.DRAFT.name());  // 默认草稿状态
        project.setIsRecommended(false);
        project.setViews(0);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        
        // 设置默认团队模式
        if (project.getTeamMode() == null) {
            project.setTeamMode("CREATE_NEW");
        }
        
        // 如果使用已有团队，设置团队ID并同步成员数量
        if ("USE_EXISTING".equals(project.getTeamMode()) && project.getTeamId() != null) {
            try {
                // 获取团队成员数量
                List<com.teamup.server.modules.team.entity.TeamMember> members = 
                    teamService.getTeamMembersByTeamId(project.getTeamId());
                project.setCurrentMembers(members != null ? members.size() : 0);
            } catch (Exception e) {
                log.error("获取团队成员数量失败", e);
                project.setCurrentMembers(0);
            }
        } else {
            project.setCurrentMembers(0);
        }
        
        projectMapper.insert(project);
        
        // 保存技能需求
        if (skillRequirements != null && !skillRequirements.isEmpty()) {
            saveSkillRequirements(project.getId(), skillRequirements);
        }

        // 保存时间段需求
        if (timeSlots != null && !timeSlots.isEmpty()) {
            saveTimeSlots(project.getId(), timeSlots);
        }
        
        // 如果使用已有团队，创建团队-项目关联
        if ("USE_EXISTING".equals(project.getTeamMode()) && project.getTeamId() != null) {
            try {
                teamProjectService.associateTeamWithProject(project.getTeamId(), project.getId());
            } catch (Exception e) {
                log.error("创建团队-项目关联失败", e);
            }
        }
        
        return project;
    }
    
    /**
     * 保存项目技能需求
     */
    private void saveSkillRequirements(Long projectId, List<Map<String, Object>> skillRequirements) {
        for (Map<String, Object> req : skillRequirements) {
            try {
                Long tagId = ((Number) req.get("tagId")).longValue();
                Boolean required = (Boolean) req.get("required");
                String proficiencyLevel = (String) req.get("proficiencyLevel");
                
                // 查询技能名称
                Tag tag = tagMapper.selectById(tagId);
                if (tag == null) {
                    log.warn("技能标签不存在: tagId={}", tagId);
                    continue;
                }
                
                ProjectSkillRequirement entity = new ProjectSkillRequirement();
                entity.setProjectId(projectId);
                entity.setSkillName(tag.getName());
                entity.setRequired(required != null ? required : false);
                entity.setProficiencyLevel(proficiencyLevel);
                entity.setCreatedAt(LocalDateTime.now());
                
                skillRequirementMapper.insert(entity);
                log.info("保存技能需求成功: projectId={}, skillName={}, required={}, level={}", 
                    projectId, tag.getName(), required, proficiencyLevel);
            } catch (Exception e) {
                log.error("保存技能需求失败: " + req, e);
            }
        }
    }

    /**
     * 保存项目时间段需求
     */
    private void saveTimeSlots(Long projectId, List<Map<String, Object>> timeSlots) {
        for (Map<String, Object> slot : timeSlots) {
            try {
                Integer dayOfWeek = ((Number) slot.get("dayOfWeek")).intValue();
                String startTime = (String) slot.get("startTime");
                String endTime = (String) slot.get("endTime");

                ProjectTimeSlot entity = new ProjectTimeSlot();
                entity.setProjectId(projectId);
                entity.setDayOfWeek(dayOfWeek);
                entity.setStartTime(LocalTime.parse(startTime));
                entity.setEndTime(LocalTime.parse(endTime));
                entity.setCreatedAt(LocalDateTime.now());

                timeSlotMapper.insert(entity);
                log.info("保存时间段成功: projectId={}, day={}, {}-{}", projectId, dayOfWeek, startTime, endTime);
            } catch (Exception e) {
                log.error("保存时间段失败: " + slot, e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(Long id, Project project, Long userId, List<Map<String, Object>> skillRequirements, List<Map<String, Object>> timeSlots) {
        Project existingProject = projectMapper.selectById(id);
        if (existingProject == null) {
            throw new BusinessException("项目不存在");
        }
        
        // 只有创建者才能修改
        if (!existingProject.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权修改此项目");
        }
        
        // 状态校验：已完成/已归档项目的编辑限制
        String existingStatus = existingProject.getStatus();
        // 是否仅做状态更新，且未携带技能/时间段更新
        boolean onlyUpdateStatus = project.getStatus() != null
                && project.getTitle() == null
                && project.getProjectType() == null
                && project.getDescription() == null
                && project.getRequirements() == null
                && project.getTeamSize() == null
                && project.getWeeklyHours() == null
                && project.getExpectedDuration() == null
                && skillRequirements == null
                && timeSlots == null;
        
        // 已完成的项目：只允许从 COMPLETED -> ARCHIVED 的状态变更
        if (ProjectStatus.COMPLETED.name().equals(existingStatus)) {
            if (!(onlyUpdateStatus && ProjectStatus.ARCHIVED.name().equals(project.getStatus()))) {
                throw new BusinessException("已完成的项目只能归档，不能再编辑其他信息");
            }
        }
        
        // 已归档的项目：不允许任何编辑
        if (ProjectStatus.ARCHIVED.name().equals(existingStatus)) {
            throw new BusinessException("已归档的项目不能再编辑");
        }
        
        // 使用条件更新，避免将未传入字段置为 null，支持部分更新（如仅更新状态）
        LambdaUpdateWrapper<Project> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Project::getId, id);
        
        boolean hasUpdateField = false;
        if (project.getTitle() != null) {
            updateWrapper.set(Project::getTitle, project.getTitle());
            hasUpdateField = true;
        }
        if (project.getProjectType() != null) {
            updateWrapper.set(Project::getProjectType, project.getProjectType());
            hasUpdateField = true;
        }
        if (project.getDescription() != null) {
            updateWrapper.set(Project::getDescription, project.getDescription());
            hasUpdateField = true;
        }
        if (project.getRequirements() != null) {
            updateWrapper.set(Project::getRequirements, project.getRequirements());
            hasUpdateField = true;
        }
        if (project.getTeamSize() != null) {
            updateWrapper.set(Project::getTeamSize, project.getTeamSize());
            hasUpdateField = true;
        }
        if (project.getWeeklyHours() != null) {
            updateWrapper.set(Project::getWeeklyHours, project.getWeeklyHours());
            hasUpdateField = true;
        }
        if (project.getExpectedDuration() != null) {
            updateWrapper.set(Project::getExpectedDuration, project.getExpectedDuration());
            hasUpdateField = true;
        }
        if (project.getStatus() != null) {
            updateWrapper.set(Project::getStatus, project.getStatus());
            hasUpdateField = true;
        }
        
        // 始终更新更新时间
        updateWrapper.set(Project::getUpdatedAt, LocalDateTime.now());
        
        if (hasUpdateField) {
            projectMapper.update(null, updateWrapper);
        }
        
        // 更新技能需求
        if (skillRequirements != null) {
            log.info("更新项目 {} 的技能需求，共 {} 个", id, skillRequirements.size());
            
            // 删除旧的技能需求
            LambdaQueryWrapper<ProjectSkillRequirement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProjectSkillRequirement::getProjectId, id);
            int deletedCount = skillRequirementMapper.delete(wrapper);
            log.info("删除了 {} 个旧的技能需求", deletedCount);
            
            // 插入新的技能需求
            for (Map<String, Object> skillReq : skillRequirements) {
                log.info("处理技能需求: {}", skillReq);
                String skillName = (String) skillReq.get("skillName");
                if (skillName == null || skillName.trim().isEmpty()) {
                    log.warn("跳过空技能名称");
                    continue;
                }
                
                ProjectSkillRequirement requirement = new ProjectSkillRequirement();
                requirement.setProjectId(id);
                requirement.setSkillName(skillName.trim());
                requirement.setRequired((Boolean) skillReq.getOrDefault("isRequired", false));
                requirement.setProficiencyLevel((String) skillReq.getOrDefault("expectedLevel", "INTERMEDIATE"));
                requirement.setCreatedAt(LocalDateTime.now());
                
                int inserted = skillRequirementMapper.insert(requirement);
                log.info("插入技能需求成功: projectId={}, skillName={}, required={}, level={}, insertResult={}", 
                    id, skillName, requirement.getRequired(), requirement.getProficiencyLevel(), inserted);
            }
        } else {
            log.info("技能需求列表为 null，不更新");
        }

        // 更新时间段需求
        if (timeSlots != null) {
            log.info("更新项目 {} 的时间段需求，共 {} 个", id, timeSlots.size());
            LambdaQueryWrapper<ProjectTimeSlot> tsWrapper = new LambdaQueryWrapper<>();
            tsWrapper.eq(ProjectTimeSlot::getProjectId, id);
            int deletedCount = timeSlotMapper.delete(tsWrapper);
            log.info("删除了 {} 个旧的时间段需求", deletedCount);
            saveTimeSlots(id, timeSlots);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id, Long userId) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        // 只有创建者才能删除
        if (!project.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权删除此项目");
        }
        
        // 只能删除草稿状态的项目
        if (!ProjectStatus.DRAFT.name().equals(project.getStatus())) {
            throw new BusinessException("只能删除草稿状态的项目");
        }
        
        projectMapper.deleteById(id);
        
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishProject(Long id, Long userId) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        if (!project.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权发布此项目");
        }
        
        if (!ProjectStatus.DRAFT.name().equals(project.getStatus())) {
            throw new BusinessException("只能发布草稿状态的项目");
        }
        
        // 如果是创建新团队模式，在发布时创建或关联团队
        if ("CREATE_NEW".equals(project.getTeamMode())) {
            if (project.getTeamId() == null) {
                try {
                    // 使用 TeamService 创建团队（会自动添加创建者为队长）
                    com.teamup.server.modules.team.dto.TeamCreateRequest teamRequest = 
                        new com.teamup.server.modules.team.dto.TeamCreateRequest();
                    teamRequest.setTeamName(project.getTitle() + " 团队");
                    teamRequest.setLeaderId(userId);
                    teamRequest.setType("PROJECT");  // 项目类型团队
                    teamRequest.setProjectId(id);
                    
                    com.teamup.server.modules.team.entity.Team team = teamService.createTeam(teamRequest);
                    
                    // 更新项目的团队ID和成员数
                    project.setTeamId(team.getId());
                    project.setCurrentMembers(1);
                    
                    // 创建团队-项目关联
                    teamProjectService.associateTeamWithProject(team.getId(), id);
                    
                    log.info("项目发布时自动创建团队：projectId={}, teamId={}", id, team.getId());
                } catch (Exception e) {
                    log.error("创建项目团队失败", e);
                    throw new BusinessException("创建项目团队失败：" + e.getMessage());
                }
            } else {
                // 已存在团队（例如创建项目时预先创建），确保团队-项目关联存在
                try {
                    teamProjectService.associateTeamWithProject(project.getTeamId(), id);
                    log.info("项目发布时为已存在团队创建关联：projectId={}, teamId={}", id, project.getTeamId());
                } catch (BusinessException e) {
                    // 如果已有关联则忽略该错误
                    log.info("项目发布时团队与项目已有关联，跳过重复关联：projectId={}, teamId={}", id, project.getTeamId());
                } catch (Exception e) {
                    log.error("项目发布时创建团队-项目关联失败：projectId={}, teamId={}, error={}", id, project.getTeamId(), e.getMessage());
                }
            }
        }
        
        project.setStatus(ProjectStatus.RECRUITING.name());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        
        // 发送异步匹配任务
        try {
            redisTemplate.opsForList().leftPush("matching_tasks", String.valueOf(id));
        } catch (Exception e) {
            // 记录日志但不中断业务
            e.printStackTrace();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectApplication applyProject(Long projectId, Long userId, String reason) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        if (!ProjectStatus.RECRUITING.name().equals(project.getStatus())) {
            throw new BusinessException("项目未在招募中");
        }
        
        // 检查是否已申请
        LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApplication::getProjectId, projectId)
               .eq(ProjectApplication::getApplicantId, userId);
        
        if (applicationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已经申请过该项目");
        }
        
        ProjectApplication application = new ProjectApplication();
        application.setProjectId(projectId);
        application.setApplicantId(userId);
        application.setApplicationReason(reason);
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());
        
        applicationMapper.insert(application);
        
        // 🔔 通知项目创建者
        try {
            com.teamup.server.modules.user.entity.User applicant = userService.getUserById(userId);
            String applicantName = applicant != null ? applicant.getUsername() : "用户";
            String projectTitle = project.getTitle() != null ? project.getTitle() : ("项目#" + projectId);
            
            notificationService.createNotification(
                project.getCreatorId(),
                "PROJECT_APPLICATION",
                applicantName + " 申请加入你的项目",
                "项目：" + projectTitle + (reason != null && !reason.isEmpty() ? "\n申请理由：" + reason : ""),
                "PROJECT",
                projectId
            );
        } catch (Exception e) {
            log.error("发送项目申请通知失败", e);
        }
        
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long applicationId, Long userId, boolean approved, String comment) {
        ProjectApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        
        // 检查是否是项目创建者
        Project project = projectMapper.selectById(application.getProjectId());
        if (!project.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权审核此申请");
        }

        if (!STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("该申请已被处理");
        }

        LocalDateTime reviewedAt = LocalDateTime.now();
        int updated = applicationMapper.update(
                null,
                new LambdaUpdateWrapper<ProjectApplication>()
                        .eq(ProjectApplication::getId, applicationId)
                        .eq(ProjectApplication::getStatus, STATUS_PENDING)
                        .set(ProjectApplication::getStatus, approved ? "APPROVED" : "REJECTED")
                        .set(ProjectApplication::getReviewedBy, userId)
                        .set(ProjectApplication::getReviewComment, comment)
                        .set(ProjectApplication::getReviewedAt, reviewedAt)
        );

        if (updated == 0) {
            // 说明状态已被他人更新，避免重复处理
            throw new BusinessException("该申请已被处理");
        }
        
        // 🔔 通知申请人
        try {
            String projectTitle = project.getTitle() != null ? project.getTitle() : ("项目#" + project.getId());
            String type = approved ? "PROJECT_APPLICATION_APPROVED" : "PROJECT_APPLICATION_REJECTED";
            String title = approved ? "你的项目申请已通过" : "你的项目申请被拒绝";
            String content = "项目：" + projectTitle + 
                           (comment != null && !comment.isEmpty() ? "\n审核意见：" + comment : "");
            
            notificationService.createNotification(
                application.getApplicantId(),
                type,
                title,
                content,
                "PROJECT",
                project.getId()
            );
        } catch (Exception e) {
            log.error("发送项目审核结果通知失败", e);
        }
    }

    @Override
    public Page<ProjectApplication> getMyProjectApplications(Long userId, int page, int size, String status) {
        // 获取用户创建的所有项目ID
        LambdaQueryWrapper<Project> projectWrapper = new LambdaQueryWrapper<>();
        projectWrapper.eq(Project::getCreatorId, userId);
        List<Project> myProjects = projectMapper.selectList(projectWrapper);
        
        if (myProjects.isEmpty()) {
            return new Page<>(page, size);
        }
        
        List<Long> projectIds = myProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());
        
        // 查询这些项目的申请
        Page<ProjectApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ProjectApplication::getProjectId, projectIds);
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(ProjectApplication::getStatus, status);
        }
        
        wrapper.orderByDesc(ProjectApplication::getAppliedAt);
        
        return applicationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<ProjectApplication> getMyApplications(Long userId, int page, int size) {
        Page<ProjectApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApplication::getApplicantId, userId);
        wrapper.orderByDesc(ProjectApplication::getAppliedAt);
        
        return applicationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReviewApplications(List<Long> applicationIds, Long userId, boolean approved, String comment) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new BusinessException("申请ID列表不能为空");
        }

        // 预先加载并校验
        List<ProjectApplication> applications = applicationMapper.selectBatchIds(applicationIds);
        if (applications.size() != applicationIds.size()) {
            throw new BusinessException("存在不存在的申请记录");
        }

        Set<Long> projectIds = applications.stream()
                .map(ProjectApplication::getProjectId)
                .collect(Collectors.toSet());

        if (projectIds.isEmpty()) {
            throw new BusinessException("申请关联的项目不存在");
        }

        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        // 权限与状态校验
        for (ProjectApplication application : applications) {
            Project project = projectMap.get(application.getProjectId());
            if (project == null) {
                throw new BusinessException("申请关联的项目不存在");
            }
            if (!Objects.equals(project.getCreatorId(), userId)) {
                throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权审核部分申请");
            }
            if (!STATUS_PENDING.equals(application.getStatus())) {
                throw new BusinessException("存在已处理的申请，无法批量操作");
            }
        }

        // 批量逐条处理，复用单条逻辑确保并发校验
        for (Long applicationId : applicationIds) {
            reviewApplication(applicationId, userId, approved, comment);
        }
    }

    @Override
    public List<Map<String, Object>> getProjectRecommendations(Long projectId, Long userId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        if (!project.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权查看此项目的推荐信息");
        }
        
        try {
            List<Map<String, Object>> recommendations = 
                recommendationMapper.selectRecommendationsWithUserInfo(projectId);
            return recommendations != null ? recommendations : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("查询项目推荐失败: projectId={}", projectId, e);
            // 返回空列表而不是抛出异常，避免前端报错
            return new java.util.ArrayList<>();
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeProject(Long projectId, Long userId, String teamAction, String summary) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        if (!project.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权完成此项目");
        }
        
        // 更新项目状态为已完成
        project.setStatus(ProjectStatus.COMPLETED.name());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        
        // 为所有项目成员增加信誉分
        if (project.getTeamId() != null) {
            try {
                List<com.teamup.server.modules.team.entity.TeamMember> members = 
                    teamService.getTeamMembersByTeamId(project.getTeamId());
                
                for (com.teamup.server.modules.team.entity.TeamMember member : members) {
                    creditService.addCreditRecord(
                        member.getUserId(),
                        10,
                        "PROJECT_COMPLETE",
                        projectId,
                        "完成项目: " + project.getTitle()
                    );
                }
                
                log.info("项目完成,为{}个成员增加信誉分", members.size());
            } catch (Exception e) {
                log.error("增加项目完成信誉分失败", e);
            }
        }
        
        // 处理团队
        if (project.getTeamId() != null) {
            // 更新 team_projects 表中的状态
            teamProjectService.completeProject(project.getTeamId(), projectId);
            
            // 创建项目履历记录
            try {
                projectHistoryService.onProjectCompleted(projectId);
                log.info("项目履历记录已创建");
            } catch (Exception e) {
                log.error("创建项目履历记录失败", e);
            }
            
            // 根据用户选择处理团队
            if ("DISSOLVE".equals(teamAction)) {
                // 解散团队
                teamService.dissolveTeam(project.getTeamId(), userId);
            } else if ("KEEP".equals(teamAction)) {
                // 保留团队，将团队性质改为长期团队
                com.teamup.server.modules.team.entity.Team team = teamService.getTeamById(project.getTeamId());
                if (team != null) {
                    team.setTeamNature("LONG_TERM");
                    team.setUpdatedAt(LocalDateTime.now());
                    teamService.updateById(team);
                }
            }
        }
        
        log.info("项目完成：projectId={}, teamAction={}", projectId, teamAction);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void associateTeamWithProject(Long projectId, Long teamId, Long userId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        if (!project.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权操作此项目");
        }
        
        com.teamup.server.modules.team.entity.Team team = teamService.getTeamById(teamId);
        if (team == null) {
            throw new BusinessException("团队不存在");
        }
        
        // 检查用户是否是团队成员
        List<com.teamup.server.modules.team.vo.TeamMemberVO> members = teamService.getTeamMembers(teamId);
        boolean isMember = members.stream().anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "你不是该团队的成员");
        }
        
        // 更新项目的团队ID
        project.setTeamId(teamId);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        
        // 在 team_projects 表中创建关联
        teamProjectService.associateTeamWithProject(teamId, projectId);
        
        log.info("项目关联团队：projectId={}, teamId={}", projectId, teamId);
    }


    @Override
    public List<ProjectSkillRequirement> getProjectSkillRequirements(Long projectId) {
        log.info("Service层查询技能需求: projectId={}", projectId);
        LambdaQueryWrapper<ProjectSkillRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectSkillRequirement::getProjectId, projectId)
               .orderByDesc(ProjectSkillRequirement::getRequired)
               .orderByAsc(ProjectSkillRequirement::getCreatedAt);
        List<ProjectSkillRequirement> result = skillRequirementMapper.selectList(wrapper);
        log.info("Service层查询到 {} 条技能需求", result != null ? result.size() : 0);
        return result;
    }

    @Override
    public List<ProjectTimeSlot> getProjectTimeSlots(Long projectId) {
        LambdaQueryWrapper<ProjectTimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectTimeSlot::getProjectId, projectId)
               .orderByAsc(ProjectTimeSlot::getDayOfWeek)
               .orderByAsc(ProjectTimeSlot::getStartTime);
        return timeSlotMapper.selectList(wrapper);
    }

}
