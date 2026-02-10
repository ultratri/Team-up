package com.teamup.server.modules.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectApplication;
import com.teamup.server.modules.project.dto.matching.MatchResult;
import com.teamup.server.modules.project.service.ApplicationService;
import com.teamup.server.modules.project.service.MatchingIntegrationService;
import com.teamup.server.modules.project.service.ProjectCommentService;
import com.teamup.server.modules.project.service.ProjectFileService;
import com.teamup.server.modules.project.service.ProjectService;
import com.teamup.server.modules.project.service.ProjectMilestoneService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 项目控制器
 */
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

    /**
     * 分页查询项目列表（只返回当前用户创建的项目）
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
     * 获取项目详情
     */
    @GetMapping("/{id}")
    public Result<Project> getProjectById(@PathVariable Long id) {
        Project project = projectService.getProjectById(id);
        return Result.success(project);
    }

    /**
     * 获取项目成员列表（通过项目关联的团队）
     */
    @GetMapping("/{id}/members")
    public Result<java.util.List<TeamMemberVO>> getProjectMembers(@PathVariable Long id) {
        // 根据 projectId 查找团队
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
     * 创建项目
     */
    @PostMapping
    public Result<Project> createProject(@RequestBody Project project) {
        // 检查当前用户角色，导师不能创建项目
        Long currentUserId = SecurityUtils.getUserId();
        if (hasRole(currentUserId, "MENTOR")) {
            return Result.error(403, "导师不能创建项目");
        }
        Project created = projectService.createProject(project, currentUserId);
        return Result.success(created);
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody Project project) {
        projectService.updateProject(id, project, SecurityUtils.getUserId());
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

