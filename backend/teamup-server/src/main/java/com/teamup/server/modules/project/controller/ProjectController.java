package com.teamup.server.modules.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectApplication;
import com.teamup.server.modules.project.dto.matching.MatchResult;
import com.teamup.server.modules.project.dto.TeamApplicationDTO;
import com.teamup.server.modules.project.dto.TeamApplicationRequest;
import com.teamup.server.modules.project.service.ApplicationService;
import com.teamup.server.modules.project.service.MatchingIntegrationService;
import com.teamup.server.modules.project.service.ProjectCommentService;
import com.teamup.server.modules.project.service.ProjectFileService;
import com.teamup.server.modules.project.service.ProjectService;
import com.teamup.server.modules.project.service.ProjectMilestoneService;
import com.teamup.server.modules.project.service.TeamApplicationService;
import com.teamup.server.modules.project.vo.ApplicationVO;
import com.teamup.server.modules.project.vo.MilestoneVO;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.team.vo.TeamMemberVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.api.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 项目控制器
 */
@Slf4j
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final MatchingIntegrationService matchingService;
    private final TeamService teamService;
    private final ProjectCommentService commentService;
    private final ProjectFileService fileService;
    private final ProjectMilestoneService milestoneService;
    private final UserService userService;
    private final TeamApplicationService teamApplicationService;

    /**
     * 分页查询项目列表（项目大厅 - 所有公开项目）
     */
    @GetMapping
    public Result<Page<Project>> getProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        // 限制分页参数范围
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;  // 防止恶意请求过大数据
        
        // 尝试获取当前用户ID，如果未登录则返回空列表
        Long currentUserId = null;
        try {
            currentUserId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 用户未登录，返回空列表
            Page<Project> emptyPage = new Page<>(page, size);
            emptyPage.setRecords(java.util.Collections.emptyList());
            emptyPage.setTotal(0);
            return Result.success(emptyPage);
        }
        
        Page<Project> result = projectService.getProjectList(page, size, type, status, keyword, currentUserId);
        return Result.success(result);
    }

    /**
     * 获取我的项目列表（只返回当前用户创建的项目）
     */
    @GetMapping("/my")
    public Result<Page<Project>> getMyProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        // 限制分页参数范围
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        
        Long currentUserId = SecurityUtils.getUserId();
        Page<Project> result = projectService.getMyProjectList(page, size, type, status, keyword, currentUserId);
        return Result.success(result);
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/{id}")
    public Result<Project> getProjectById(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Project project = projectService.getProjectById(id);
        
        // 异步增加浏览次数（带防刷机制）
        if (project != null) {
            try {
                Long userId = SecurityUtils.getUserId();
                String ipAddress = getClientIpAddress(request);
                projectService.incrementProjectViews(id, userId, ipAddress);
            } catch (Exception e) {
                // 增加浏览次数失败不影响主流程
                // 用户可能未登录，使用 IP 地址
                try {
                    String ipAddress = getClientIpAddress(request);
                    projectService.incrementProjectViews(id, null, ipAddress);
                } catch (Exception ex) {
                    // 忽略错误
                }
            }
        }
        
        return Result.success(project);
    }
    
    /**
     * 获取客户端真实 IP 地址
     */
    private String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个 IP 的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取项目成员列表（通过项目关联的团队）
     */
    @GetMapping("/{id}/members")
    public Result<java.util.List<TeamMemberVO>> getProjectMembers(@PathVariable Long id) {
        // 先查询项目，获取关联的团队ID
        Project project = projectService.getProjectById(id);
        if (project == null) {
            return Result.success(java.util.Collections.emptyList());
        }
        
        // 如果项目有关联的团队ID，直接使用
        if (project.getTeamId() != null) {
            java.util.List<TeamMemberVO> members = teamService.getTeamMembers(project.getTeamId());
            return Result.success(members);
        }
        
        // 兼容旧逻辑：通过 projectId 查找团队
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Team::getProjectId, id);
        Team team = teamService.getOne(wrapper);

        if (team == null) {
            // 尚未为该项目创建团队时返回空列表
            return Result.success(java.util.Collections.emptyList());
        }

        java.util.List<TeamMemberVO> members = teamService.getTeamMembers(team.getId());
        return Result.success(members);
    }

    /**
     * 获取项目技能需求
     */
    @GetMapping("/{id}/skill-requirements")
    public Result<List<com.teamup.server.modules.project.entity.ProjectSkillRequirement>> getProjectSkillRequirements(@PathVariable Long id) {
        log.info("查询项目技能需求: projectId={}", id);
        List<com.teamup.server.modules.project.entity.ProjectSkillRequirement> requirements =
            projectService.getProjectSkillRequirements(id);
        log.info("返回技能需求数量: {}", requirements != null ? requirements.size() : 0);
        if (requirements != null && !requirements.isEmpty()) {
            log.info("技能需求详情: {}", requirements);
        }
        return Result.success(requirements);
    }

    /**
     * 获取项目时间段需求
     */
    @GetMapping("/{id}/time-slots")
    public Result<List<com.teamup.server.modules.project.entity.ProjectTimeSlot>> getProjectTimeSlots(@PathVariable Long id) {
        List<com.teamup.server.modules.project.entity.ProjectTimeSlot> timeSlots =
            projectService.getProjectTimeSlots(id);
        return Result.success(timeSlots);
    }


    /**
     * 创建项目
     */
    @PostMapping
    public Result<Project> createProject(@RequestBody java.util.Map<String, Object> requestBody) {
        // 检查当前用户角色，导师不能创建项目
        Long currentUserId = SecurityUtils.getUserId();
        if (hasRole(currentUserId, "MENTOR")) {
            return Result.error(403, "导师不能创建项目");
        }
        
        // 构建 Project 对象
        Project project = new Project();
        project.setTitle((String) requestBody.get("title"));
        project.setProjectType((String) requestBody.get("projectType"));
        project.setDescription((String) requestBody.get("description"));
        project.setRequirements((String) requestBody.get("requirements"));
        project.setTeamSize(requestBody.get("teamSize") != null ? ((Number) requestBody.get("teamSize")).intValue() : 5);
        project.setWeeklyHours(requestBody.get("weeklyHours") != null ? ((Number) requestBody.get("weeklyHours")).intValue() : 10);
        project.setExpectedDuration(requestBody.get("expectedDuration") != null ? ((Number) requestBody.get("expectedDuration")).intValue() : 30);
        project.setTeamMode((String) requestBody.get("teamMode"));
        
        // 处理 existingTeamId -> teamId 的映射
        if (requestBody.containsKey("existingTeamId") && requestBody.get("existingTeamId") != null) {
            project.setTeamId(((Number) requestBody.get("existingTeamId")).longValue());
        }
        
        // 提取技能需求列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skillRequirements = (List<Map<String, Object>>) requestBody.get("skillRequirements");

        // 提取时间段需求列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> timeSlots = (List<Map<String, Object>>) requestBody.get("timeSlots");

        // 提取邀请队友信息
        Boolean inviteTeammates = requestBody.get("inviteTeammates") != null ? 
            (Boolean) requestBody.get("inviteTeammates") : false;
        @SuppressWarnings("unchecked")
        List<Number> invitedUserIds = inviteTeammates && requestBody.get("invitedUserIds") != null ? 
            (List<Number>) requestBody.get("invitedUserIds") : null;

        // 如果是创建新团队模式且勾选了邀请队友，在创建项目前预创建团队
        Long preCreatedTeamId = null;
        String teamMode = project.getTeamMode();
        if ("CREATE_NEW".equals(teamMode)
                && inviteTeammates
                && invitedUserIds != null
                && !invitedUserIds.isEmpty()
                && project.getTeamId() == null) {
            String teamNameFromRequest = requestBody.get("teamName") != null
                    ? (String) requestBody.get("teamName")
                    : project.getTitle();
            String teamName = (teamNameFromRequest != null && !teamNameFromRequest.isBlank())
                    ? teamNameFromRequest
                    : project.getTitle();
            try {
                com.teamup.server.modules.team.dto.TeamCreateRequest teamRequest =
                        new com.teamup.server.modules.team.dto.TeamCreateRequest();
                teamRequest.setTeamName(teamName);
                teamRequest.setLeaderId(currentUserId);
                teamRequest.setType("PROJECT");
                Integer teamSize = project.getTeamSize();
                if (teamSize != null && teamSize > 0) {
                    teamRequest.setMaxMembers(teamSize);
                }
                com.teamup.server.modules.team.entity.Team team = teamService.createTeam(teamRequest);
                preCreatedTeamId = team.getId();
                // 将预创建团队写入项目，后续发布时不会再次创建
                project.setTeamId(preCreatedTeamId);
                log.info("为新项目预创建团队成功: teamId={}, teamName={}", preCreatedTeamId, teamName);
            } catch (Exception e) {
                log.error("为新项目预创建团队失败，将继续仅创建项目: {}", e.getMessage(), e);
            }
        }

        Project created = projectService.createProject(project, currentUserId, skillRequirements, timeSlots);
        
        Long targetTeamId = created.getTeamId() != null ? created.getTeamId() : preCreatedTeamId;

        // 如果需要邀请队友且已经有可用的团队
        if (inviteTeammates && invitedUserIds != null && !invitedUserIds.isEmpty() && targetTeamId != null) {
            int successCount = 0;
            int failCount = 0;
            for (Number userId : invitedUserIds) {
                try {
                    log.info("开始邀请队友: projectId={}, teamId={}, inviterId={}, inviteeId={}", 
                            created.getId(), targetTeamId, currentUserId, userId);
                    
                    teamService.inviteMember(
                        targetTeamId, 
                        currentUserId, 
                        userId.longValue(),
                        "邀请你加入项目「" + created.getTitle() + "」的团队"
                    );
                    
                    successCount++;
                    log.info("邀请队友成功: userId={}", userId);
                } catch (Exception e) {
                    failCount++;
                    log.error("邀请队友失败: projectId={}, teamId={}, userId={}, error={}", 
                            created.getId(), targetTeamId, userId, e.getMessage(), e);
                }
            }
            log.info("项目创建成功，邀请结果: 成功 {} 位，失败 {} 位", successCount, failCount);
        }
        
        return Result.success(created);
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        log.info("收到更新项目请求: projectId={}, requestBody keys={}", id, requestBody.keySet());
        
        Project project = new Project();
        // 仅在请求中包含对应字段时才设置，便于支持部分更新
        if (requestBody.containsKey("title")) {
            project.setTitle((String) requestBody.get("title"));
        }
        if (requestBody.containsKey("projectType")) {
            project.setProjectType((String) requestBody.get("projectType"));
        }
        if (requestBody.containsKey("description")) {
            project.setDescription((String) requestBody.get("description"));
        }
        if (requestBody.containsKey("requirements")) {
            project.setRequirements((String) requestBody.get("requirements"));
        }
        if (requestBody.containsKey("teamSize")) {
            project.setTeamSize((Integer) requestBody.get("teamSize"));
        }
        if (requestBody.containsKey("weeklyHours")) {
            project.setWeeklyHours((Integer) requestBody.get("weeklyHours"));
        }
        if (requestBody.containsKey("expectedDuration")) {
            project.setExpectedDuration((Integer) requestBody.get("expectedDuration"));
        }
        if (requestBody.containsKey("status")) {
            project.setStatus((String) requestBody.get("status"));
        }
        
        // 提取技能需求列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skillRequirements = (List<Map<String, Object>>) requestBody.get("skillRequirements");
        log.info("技能需求列表: {}", skillRequirements);

        // 提取时间段需求列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> timeSlots = (List<Map<String, Object>>) requestBody.get("timeSlots");

        projectService.updateProject(id, project, SecurityUtils.getUserId(), skillRequirements, timeSlots);
        return Result.success(null);
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id, SecurityUtils.getUserId());
        return Result.success(null);
    }

    /**
     * 发布项目
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publishProject(@PathVariable Long id) {
        projectService.publishProject(id, SecurityUtils.getUserId());
        return Result.success(null);
    }

    /**
     * 申请加入项目
     */
    @PostMapping("/{id}/apply")
    public Result<ProjectApplication> applyProject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        String reason = payload != null ? (String) payload.get("reason") : null;
        ProjectApplication application = projectService.applyProject(id, SecurityUtils.getUserId(), reason);
        return Result.success(application);
    }

    /**
     * 团队申请加入项目
     */
    @PostMapping("/{id}/team-apply")
    public Result<TeamApplicationDTO> teamApplyProject(
            @PathVariable Long id,
            @RequestBody TeamApplicationRequest request
    ) {
        Long leaderId = SecurityUtils.getUserId();
        TeamApplicationDTO application = teamApplicationService.createTeamApplication(id, leaderId, request);
        return Result.success(application);
    }

    /**
     * 获取团队申请详情
     */
    @GetMapping("/team-applications/{id}")
    public Result<TeamApplicationDTO> getTeamApplication(@PathVariable Long id) {
        TeamApplicationDTO application = teamApplicationService.getTeamApplication(id);
        return Result.success(application);
    }

    /**
     * 获取项目的所有团队申请
     */
    @GetMapping("/{id}/team-applications")
    public Result<List<TeamApplicationDTO>> getProjectTeamApplications(@PathVariable Long id) {
        List<TeamApplicationDTO> applications = teamApplicationService.getProjectTeamApplications(id);
        return Result.success(applications);
    }

    /**
     * 获取我的团队申请历史
     */
    @GetMapping("/team-applications/my")
    public Result<List<TeamApplicationDTO>> getMyTeamApplications() {
        Long userId = SecurityUtils.getUserId();
        List<TeamApplicationDTO> applications = teamApplicationService.getUserTeamApplications(userId);
        return Result.success(applications);
    }

    /**
     * 确认参与团队申请
     */
    @PostMapping("/team-applications/{id}/confirm")
    public Result<Void> confirmTeamMembership(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        teamApplicationService.confirmMembership(id, userId);
        return Result.success(null);
    }

    /**
     * 审核团队申请
     */
    @PostMapping("/team-applications/{id}/review")
    public Result<Void> reviewTeamApplication(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comment
    ) {
        Long reviewerId = SecurityUtils.getUserId();
        teamApplicationService.reviewTeamApplication(id, reviewerId, approved, comment);
        return Result.success(null);
    }

    /**
     * 取消团队申请
     */
    @PostMapping("/team-applications/{id}/cancel")
    public Result<Void> cancelTeamApplication(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        teamApplicationService.cancelTeamApplication(id, userId);
        return Result.success(null);
    }

    /**
     * 审核申请
     */
    @PostMapping("/applications/{id}/review")
    public Result<Void> reviewApplication(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comment
    ) {
        projectService.reviewApplication(id, SecurityUtils.getUserId(), approved, comment);
        return Result.success(null);
    }

    /**
     * 获取我创建的项目的所有申请
     */
    @GetMapping("/applications/my-projects")
    public Result<Page<ApplicationVO>> getMyProjectApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        // 复用 ApplicationService 的 VO 转换逻辑
        // 注意：这里返回“我创建的项目”的所有申请（由 ProjectService 负责筛选项目ID）
        Page<ProjectApplication> raw = projectService.getMyProjectApplications(SecurityUtils.getUserId(), page, size, status);
        Page<ApplicationVO> result = new Page<>(page, size, raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(a -> applicationService.getApplicationDetail(a.getId())).toList());
        return Result.success(result);
    }

    /**
     * 获取我的申请历史
     */
    @GetMapping("/applications/my-applications")
    public Result<Page<ApplicationVO>> getMyApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<ProjectApplication> raw = projectService.getMyApplications(SecurityUtils.getUserId(), page, size);
        Page<ApplicationVO> result = new Page<>(page, size, raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(a -> applicationService.getApplicationDetail(a.getId())).toList());
        return Result.success(result);
    }

    /**
     * 撤回申请（仅申请人可撤回，且仅限 PENDING）
     */
    @PostMapping("/applications/{id}/withdraw")
    public Result<Void> withdrawApplication(@PathVariable Long id) {
        applicationService.withdrawApplication(id, SecurityUtils.getUserId());
        return Result.success(null);
    }

    /**
     * 批量审核申请
     */
    @PostMapping("/applications/batch-review")
    public Result<Void> batchReviewApplications(
            @RequestBody Map<String, Object> request
    ) {
        @SuppressWarnings("unchecked")
        List<Long> applicationIds = (List<Long>) request.get("applicationIds");
        Boolean approved = (Boolean) request.get("approved");
        String comment = (String) request.get("comment");
        
        projectService.batchReviewApplications(
            applicationIds, SecurityUtils.getUserId(), approved, comment
        );
        return Result.success(null);
    }

    /**
     * 智能匹配推荐 (旧接口，保留兼容)
     */
    @PostMapping("/{id}/match")
    public Result<List<MatchResult>> matchCandidates(@PathVariable Long id) {
        return Result.success(matchingService.matchCandidates(id));
    }

    /**
     * 获取项目推荐候选人 (读取预计算结果 - 新接口)
     */
    @GetMapping("/{id}/recommendations")
    public Result<List<Map<String, Object>>> getRecommendations(@PathVariable Long id) {
        return Result.success(projectService.getProjectRecommendations(id, SecurityUtils.getUserId()));
    }

    /**
     * 分页获取项目评论
     */
    @GetMapping("/{id}/comments")
    public Result<Page<com.teamup.server.modules.project.vo.ProjectCommentVO>> getProjectComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        Page<com.teamup.server.modules.project.vo.ProjectCommentVO> result =
                commentService.getProjectComments(id, page, size);
        return Result.success(result);
    }

    /**
     * 新增评论或回复
     */
    @PostMapping("/{id}/comments")
    public Result<com.teamup.server.modules.project.vo.ProjectCommentVO> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        Long userId = SecurityUtils.getUserId();
        Long parentId = request.get("parentId") != null
                ? ((Number) request.get("parentId")).longValue()
                : null;
        Long replyToUserId = request.get("replyToUserId") != null
                ? ((Number) request.get("replyToUserId")).longValue()
                : null;
        String content = (String) request.get("content");

        com.teamup.server.modules.project.vo.ProjectCommentVO vo =
                commentService.addComment(id, userId, parentId, replyToUserId, content);
        return Result.success(vo);
    }

    /**
     * 获取项目文件列表（分页）
     */
    @GetMapping("/{id}/files")
    public Result<Page<com.teamup.server.modules.project.vo.ProjectFileVO>> getProjectFiles(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category
    ) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        
        Page<com.teamup.server.modules.project.vo.ProjectFileVO> result =
                fileService.getProjectFiles(id, category, page, size);
        return Result.success(result);
    }

    /**
     * 上传项目文件
     */
    @PostMapping("/{id}/files")
    public Result<com.teamup.server.modules.project.vo.ProjectFileVO> uploadProjectFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description
    ) {
        Long userId = SecurityUtils.getUserId();
        com.teamup.server.modules.project.vo.ProjectFileVO vo =
                fileService.uploadFile(id, userId, file, category, description);
        return Result.success(vo);
    }

    /**
     * 删除项目文件
     */
    @DeleteMapping("/files/{fileId}")
    public Result<Void> deleteProjectFile(@PathVariable Long fileId) {
        Long userId = SecurityUtils.getUserId();
        fileService.deleteFile(fileId, userId);
        return Result.success(null);
    }

    /**
     * 获取项目文件分类列表
     */
    @GetMapping("/{id}/files/categories")
    public Result<List<String>> getFileCategories(@PathVariable Long id) {
        List<String> categories = fileService.getFileCategories(id);
        return Result.success(categories);
    }

    /**
     * 获取项目里程碑列表
     */
    @GetMapping("/{id}/milestones")
    public Result<List<MilestoneVO>> getMilestones(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(milestoneService.listByProject(id, userId));
    }

    /**
     * 新增里程碑
     */
    @PostMapping("/{id}/milestones")
    public Result<MilestoneVO> createMilestone(
            @PathVariable Long id,
            @RequestBody MilestoneVO payload
    ) {
        Long userId = SecurityUtils.getUserId();
        MilestoneVO vo = milestoneService.createMilestone(id, userId, payload);
        return Result.success(vo);
    }

    /**
     * 更新里程碑
     */
    @PutMapping("/milestones/{milestoneId}")
    public Result<MilestoneVO> updateMilestone(
            @PathVariable Long milestoneId,
            @RequestBody MilestoneVO payload
    ) {
        Long userId = SecurityUtils.getUserId();
        MilestoneVO vo = milestoneService.updateMilestone(milestoneId, userId, payload);
        return Result.success(vo);
    }

    /**
     * 删除里程碑
     */
    @DeleteMapping("/milestones/{milestoneId}")
    public Result<Void> deleteMilestone(@PathVariable Long milestoneId) {
        Long userId = SecurityUtils.getUserId();
        milestoneService.deleteMilestone(milestoneId, userId);
        return Result.success(null);
    }
    
    /**
     * 完成项目（并处理团队）
     */
    @PostMapping("/{id}/complete")
    public Result<Void> completeProject(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        Long userId = SecurityUtils.getUserId();
        String teamAction = (String) request.get("teamAction"); // KEEP 或 DISSOLVE
        String summary = (String) request.get("summary");
        
        projectService.completeProject(id, userId, teamAction, summary);
        return Result.success(null);
    }
    
    /**
     * 为项目关联团队
     */
    @PostMapping("/{id}/associate-team")
    public Result<Void> associateTeam(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        Long userId = SecurityUtils.getUserId();
        Long teamId = ((Number) request.get("teamId")).longValue();
        
        projectService.associateTeamWithProject(id, teamId, userId);
        return Result.success(null);
    }
    
    /**
     * 成员找项目：为当前用户匹配合适的项目
     */
    @GetMapping("/match-for-me")
    public Result<List<com.teamup.server.modules.project.dto.matching.ProjectWithMatchScore>> matchProjectsForCurrentUser(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = SecurityUtils.getUserId();
        List<com.teamup.server.modules.project.dto.matching.ProjectWithMatchScore> results = 
                matchingService.matchProjectsForUser(userId, page, size);
        return Result.success(results);
    }
    
    /**
     * 检查用户是否具有指定角色
     */
    private boolean hasRole(Long userId, String roleName) {
        User user = userService.getUserById(userId);
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains(roleName);
    }

}

