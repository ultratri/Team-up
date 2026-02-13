package com.teamup.server.modules.mentor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.mentor.dto.MentorApplicationRequest;
import com.teamup.server.modules.mentor.dto.ReviewApplicationRequest;
import com.teamup.server.modules.mentor.entity.MentorApplication;
import com.teamup.server.modules.mentor.mapper.MentorApplicationMapper;
import com.teamup.server.modules.mentor.service.MentorApplicationService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserRole;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导师申请服务实现
 */
@Slf4j
@Service
public class MentorApplicationServiceImpl implements MentorApplicationService {

    @Autowired
    private MentorApplicationMapper applicationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper roleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MentorApplication submitApplication(Long userId, MentorApplicationRequest request) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证用户是否已经是导师
        List<String> roles = roleMapper.getUserRoles(userId);
        if (roles != null && roles.contains("MENTOR")) {
            throw new BusinessException("您已经是导师，无需重复申请");
        }

        // 验证是否有待审核的申请
        LambdaQueryWrapper<MentorApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MentorApplication::getApplicantId, userId)
               .eq(MentorApplication::getStatus, "PENDING");
        if (applicationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您有待审核的申请，请等待审核结果");
        }

        // 验证用户编号是否已被其他人使用
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MentorApplication::getUserCode, request.getUserCode())
               .ne(MentorApplication::getApplicantId, userId);
        if (applicationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该用户编号已被使用");
        }

        // 创建申请记录
        MentorApplication application = new MentorApplication();
        application.setApplicantId(userId);
        application.setRealName(request.getRealName());
        application.setUserCode(request.getUserCode());
        application.setDepartment(request.getDepartment());
        application.setMajor(request.getMajor());
        application.setEmail(request.getEmail());
        application.setPhone(request.getPhone());
        application.setBio(request.getBio());
        application.setProjectExperience(request.getProjectExperience());
        application.setGuidanceExperience(request.getGuidanceExperience());
        application.setApplicationReason(request.getApplicationReason());
        application.setStatus("PENDING");
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        applicationMapper.insert(application);

        log.info("用户 {} 提交了导师申请，申请ID: {}", userId, application.getId());
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long applicationId, Long reviewerId, ReviewApplicationRequest request) {
        // 获取申请记录
        MentorApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }

        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("该申请已被审核");
        }

        // 更新申请状态
        String status = request.getApproved() ? "APPROVED" : "REJECTED";
        application.setStatus(status);
        application.setReviewerId(reviewerId);
        application.setReviewComment(request.getReviewComment());
        application.setReviewedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        applicationMapper.updateById(application);

        // 如果审核通过，添加导师角色
        if (request.getApproved()) {
            // 添加导师角色
            UserRole userRole = new UserRole();
            userRole.setUserId(application.getApplicantId());
            userRole.setRoleName("MENTOR");
            roleMapper.insert(userRole);

            log.info("用户 {} 的导师申请已通过，已添加导师角色", application.getApplicantId());
        } else {
            log.info("用户 {} 的导师申请已拒绝", application.getApplicantId());
        }
    }

    @Override
    public Page<MentorApplication> getApplicationList(Integer page, Integer size, String status) {
        Page<MentorApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MentorApplication> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(MentorApplication::getStatus, status);
        }

        wrapper.orderByDesc(MentorApplication::getCreatedAt);
        return applicationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public MentorApplication getUserApplication(Long userId) {
        LambdaQueryWrapper<MentorApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MentorApplication::getApplicantId, userId)
               .orderByDesc(MentorApplication::getCreatedAt)
               .last("LIMIT 1");
        return applicationMapper.selectOne(wrapper);
    }

    @Override
    public boolean canApply(Long userId) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        // 检查是否已经是导师
        List<String> roles = roleMapper.getUserRoles(userId);
        if (roles != null && roles.contains("MENTOR")) {
            return false;
        }

        // 检查是否有待审核的申请
        LambdaQueryWrapper<MentorApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MentorApplication::getApplicantId, userId)
               .eq(MentorApplication::getStatus, "PENDING");
        return applicationMapper.selectCount(wrapper) == 0;
    }
}
