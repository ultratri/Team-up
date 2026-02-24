package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.teamup.server.common.api.ApiErrorCode;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectApplication;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.project.mapper.ProjectApplicationMapper;
import com.teamup.server.modules.project.service.ApplicationService;
import com.teamup.server.modules.project.vo.ApplicationVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.tag.service.UserTagService;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目申请管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ProjectApplicationMapper applicationMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserTagService userTagService;
    
    private static final String STATUS_PENDING = "PENDING";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectApplication createApplication(Long projectId, Long userId, String reason) {
        // 检查项目是否存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        // 检查项目状态
        if (!"RECRUITING".equals(project.getStatus())) {
            throw new BusinessException("项目未在招募中");
        }
        
        // 检查是否已申请
        LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApplication::getProjectId, projectId)
               .eq(ProjectApplication::getApplicantId, userId)
               .in(ProjectApplication::getStatus, "PENDING", "APPROVED");
        
        if (applicationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已经申请过该项目或已是成员");
        }
        
        // 创建申请
        ProjectApplication application = new ProjectApplication();
        application.setProjectId(projectId);
        application.setApplicantId(userId);
        application.setApplicationReason(reason);
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());
        
        applicationMapper.insert(application);
        log.info("用户 {} 申请加入项目 {}", userId, projectId);
        
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long applicationId, Long reviewerId, boolean approved, String comment) {
        ProjectApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        
        if (!STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("该申请已被处理");
        }
        
        // 检查权限
        Project project = projectMapper.selectById(application.getProjectId());
        if (!project.getCreatorId().equals(reviewerId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权审核此申请");
        }
        
        LocalDateTime reviewedAt = LocalDateTime.now();
        int updated = applicationMapper.update(
                null,
                new LambdaUpdateWrapper<ProjectApplication>()
                        .eq(ProjectApplication::getId, applicationId)
                        .eq(ProjectApplication::getStatus, STATUS_PENDING)
                        .set(ProjectApplication::getStatus, approved ? "APPROVED" : "REJECTED")
                        .set(ProjectApplication::getReviewedBy, reviewerId)
                        .set(ProjectApplication::getReviewComment, comment)
                        .set(ProjectApplication::getReviewedAt, reviewedAt)
        );

        if (updated == 0) {
            throw new BusinessException("该申请已被处理");
        }
        log.info("审核者 {} {} 了用户 {} 的申请 {}", reviewerId, approved ? "通过" : "拒绝", 
                application.getApplicantId(), applicationId);
        
        // TODO: 发送通知给申请人
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReviewApplications(List<Long> applicationIds, Long reviewerId, boolean approved, String comment) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new BusinessException("申请ID列表不能为空");
        }
        
        for (Long applicationId : applicationIds) {
            reviewApplication(applicationId, reviewerId, approved, comment);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApplication(Long applicationId, Long userId) {
        ProjectApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权撤回此申请");
        }
        
        if (!STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("该申请已被处理，无法撤回");
        }
        
        LocalDateTime reviewedAt = LocalDateTime.now();
        int updated = applicationMapper.update(
                null,
                new LambdaUpdateWrapper<ProjectApplication>()
                        .eq(ProjectApplication::getId, applicationId)
                        .eq(ProjectApplication::getApplicantId, userId)
                        .eq(ProjectApplication::getStatus, STATUS_PENDING)
                        .set(ProjectApplication::getStatus, "WITHDRAWN")
                        .set(ProjectApplication::getReviewedAt, reviewedAt)
        );

        if (updated == 0) {
            throw new BusinessException("该申请已被处理，无法撤回");
        }
        
        log.info("用户 {} 撤回了申请 {}", userId, applicationId);
    }

    @Override
    public Page<ApplicationVO> getProjectApplications(Long projectId, int page, int size, String status) {
        Page<ProjectApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApplication::getProjectId, projectId);
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(ProjectApplication::getStatus, status);
        }
        
        wrapper.orderByDesc(ProjectApplication::getAppliedAt);
        
        Page<ProjectApplication> appPage = applicationMapper.selectPage(pageParam, wrapper);
        
        // 转换为VO
        Page<ApplicationVO> voPage = new Page<>(page, size, appPage.getTotal());
        voPage.setRecords(appPage.getRecords().stream().map(this::convertToVO).toList());
        
        return voPage;
    }

    @Override
    public Page<ApplicationVO> getUserApplications(Long userId, int page, int size, String status) {
        Page<ProjectApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApplication::getApplicantId, userId);
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(ProjectApplication::getStatus, status);
        }
        
        wrapper.orderByDesc(ProjectApplication::getAppliedAt);
        
        Page<ProjectApplication> appPage = applicationMapper.selectPage(pageParam, wrapper);
        
        // 转换为VO
        Page<ApplicationVO> voPage = new Page<>(page, size, appPage.getTotal());
        voPage.setRecords(appPage.getRecords().stream().map(this::convertToVO).toList());
        
        return voPage;
    }

    @Override
    public Long getPendingCount(Long projectId) {
        LambdaQueryWrapper<ProjectApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApplication::getProjectId, projectId)
               .eq(ProjectApplication::getStatus, "PENDING");
        
        return applicationMapper.selectCount(wrapper);
    }

    @Override
    public ApplicationVO getApplicationDetail(Long applicationId) {
        ProjectApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        
        return convertToVO(application);
    }

    private ApplicationVO convertToVO(ProjectApplication application) {
        ApplicationVO vo = new ApplicationVO();
        vo.setId(application.getId());
        vo.setProjectId(application.getProjectId());
        vo.setApplicantId(application.getApplicantId());
        vo.setApplicationReason(application.getApplicationReason());
        vo.setStatus(application.getStatus());
        vo.setReviewedBy(application.getReviewedBy());
        vo.setReviewComment(application.getReviewComment());
        vo.setAppliedAt(application.getAppliedAt());
        vo.setReviewedAt(application.getReviewedAt());
        
        // 填充项目信息
        Project project = projectMapper.selectById(application.getProjectId());
        if (project != null) {
            vo.setProjectTitle(project.getTitle());
        }
        
        // 填充申请人信息
        User applicant = userMapper.selectById(application.getApplicantId());
        if (applicant != null) {
            vo.setApplicantName(applicant.getUsername());
        }

        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, application.getApplicantId())
        );
        if (profile != null) {
            vo.setApplicantAvatar(profile.getAvatarUrl());
        }

        // 使用 UserTagService 获取用户技能
        List<UserSkillVO> skills = userTagService.getUserSkills(application.getApplicantId());
        if (skills != null && !skills.isEmpty()) {
            vo.setApplicantSkills(skills.stream()
                    .map(UserSkillVO::getTagName)
                    .toList());
        }
        
        // 填充审核人信息
        if (application.getReviewedBy() != null) {
            User reviewer = userMapper.selectById(application.getReviewedBy());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getUsername());
            }
        }
        
        return vo;
    }
}
