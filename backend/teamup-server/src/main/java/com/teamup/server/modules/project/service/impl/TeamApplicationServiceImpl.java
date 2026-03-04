package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.project.dto.TeamApplicationDTO;
import com.teamup.server.modules.project.dto.TeamApplicationRequest;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.TeamApplication;
import com.teamup.server.modules.project.entity.TeamApplicationMember;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.project.mapper.TeamApplicationMapper;
import com.teamup.server.modules.project.mapper.TeamApplicationMemberMapper;
import com.teamup.server.modules.project.service.TeamApplicationService;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 团队申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamApplicationServiceImpl implements TeamApplicationService {
    
    private final TeamApplicationMapper teamApplicationMapper;
    private final TeamApplicationMemberMapper memberMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final TeamService teamService;
    private final NotificationService notificationService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamApplicationDTO createTeamApplication(Long projectId, Long leaderId, TeamApplicationRequest request) {
        // 验证项目存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        // 验证项目状态
        if (!"RECRUITING".equals(project.getStatus())) {
            throw new BusinessException("项目当前不接受申请");
        }
        
        // 验证申请人列表
        if (request.getApplicantIds() == null || request.getApplicantIds().isEmpty()) {
            throw new BusinessException("申请人列表不能为空");
        }
        
        // 验证发起人在申请人列表中
        if (!request.getApplicantIds().contains(leaderId)) {
            throw new BusinessException("发起人必须在申请人列表中");
        }
        
        // 创建团队申请
        TeamApplication application = new TeamApplication();
        application.setProjectId(projectId);
        application.setLeaderId(leaderId);
        application.setMessage(request.getMessage());
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());
        
        teamApplicationMapper.insert(application);
        
        // 创建团队成员记录
        for (Long userId : request.getApplicantIds()) {
            TeamApplicationMember member = new TeamApplicationMember();
            member.setTeamApplicationId(application.getId());
            member.setUserId(userId);
            // 发起人自动确认
            if (userId.equals(leaderId)) {
                member.setConfirmed(true);
                member.setConfirmedAt(LocalDateTime.now());
            } else {
                member.setConfirmed(false);
            }
            memberMapper.insert(member);
        }
        
        log.info("创建团队申请成功: applicationId={}, projectId={}, leaderId={}, memberCount={}", 
                application.getId(), projectId, leaderId, request.getApplicantIds().size());
        
        // 发送通知给被邀请的队友（除了发起人）
        User leader = userMapper.selectById(leaderId);
        String leaderName = leader != null ? (leader.getNickname() != null ? leader.getNickname() : leader.getUsername()) : "用户";
        
        for (Long userId : request.getApplicantIds()) {
            if (!userId.equals(leaderId)) {
                notificationService.createNotification(
                    userId,
                    "TEAM_APPLICATION_INVITE",
                    "团队申请邀请",
                    String.format("%s 邀请你一起申请项目「%s」，请尽快确认", leaderName, project.getTitle()),
                    "TEAM_APPLICATION",
                    application.getId()
                );
            }
        }
        
        return getTeamApplication(application.getId());
    }
    
    @Override
    public TeamApplicationDTO getTeamApplication(Long applicationId) {
        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("团队申请不存在");
        }
        
        return convertToDTO(application);
    }
    
    @Override
    public List<TeamApplicationDTO> getProjectTeamApplications(Long projectId) {
        LambdaQueryWrapper<TeamApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamApplication::getProjectId, projectId)
                .orderByDesc(TeamApplication::getAppliedAt);
        
        List<TeamApplication> applications = teamApplicationMapper.selectList(wrapper);
        return applications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TeamApplicationDTO> getUserTeamApplications(Long userId) {
        // 查询用户作为发起人的申请
        LambdaQueryWrapper<TeamApplication> leaderWrapper = new LambdaQueryWrapper<>();
        leaderWrapper.eq(TeamApplication::getLeaderId, userId)
                .orderByDesc(TeamApplication::getAppliedAt);
        List<TeamApplication> leaderApplications = teamApplicationMapper.selectList(leaderWrapper);
        
        // 查询用户作为成员的申请
        LambdaQueryWrapper<TeamApplicationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamApplicationMember::getUserId, userId);
        List<TeamApplicationMember> members = memberMapper.selectList(memberWrapper);
        
        List<Long> applicationIds = members.stream()
                .map(TeamApplicationMember::getTeamApplicationId)
                .collect(Collectors.toList());
        
        List<TeamApplication> memberApplications = new ArrayList<>();
        if (!applicationIds.isEmpty()) {
            memberApplications = teamApplicationMapper.selectBatchIds(applicationIds);
        }
        
        // 合并并去重
        List<TeamApplication> allApplications = new ArrayList<>(leaderApplications);
        for (TeamApplication app : memberApplications) {
            if (allApplications.stream().noneMatch(a -> a.getId().equals(app.getId()))) {
                allApplications.add(app);
            }
        }
        
        // 按申请时间倒序排序
        allApplications.sort((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()));
        
        return allApplications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmMembership(Long applicationId, Long userId) {
        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("团队申请不存在");
        }
        
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("申请已处理，无法确认");
        }
        
        // 查询成员记录
        LambdaQueryWrapper<TeamApplicationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamApplicationMember::getTeamApplicationId, applicationId)
                .eq(TeamApplicationMember::getUserId, userId);
        TeamApplicationMember member = memberMapper.selectOne(wrapper);
        
        if (member == null) {
            throw new BusinessException("你不在该团队申请中");
        }
        
        if (Boolean.TRUE.equals(member.getConfirmed())) {
            throw new BusinessException("你已经确认过了");
        }
        
        // 更新确认状态
        member.setConfirmed(true);
        member.setConfirmedAt(LocalDateTime.now());
        memberMapper.updateById(member);
        
        log.info("成员确认团队申请: applicationId={}, userId={}", applicationId, userId);
        
        // 发送通知给发起人
        User confirmer = userMapper.selectById(userId);
        String confirmerName = confirmer != null ? (confirmer.getNickname() != null ? confirmer.getNickname() : confirmer.getUsername()) : "队友";
        
        Project project = projectMapper.selectById(application.getProjectId());
        notificationService.createNotification(
            application.getLeaderId(),
            "TEAM_APPLICATION_CONFIRMED",
            "队友已确认",
            String.format("%s 已确认参与项目「%s」的团队申请", confirmerName, project != null ? project.getTitle() : ""),
            "TEAM_APPLICATION",
            applicationId
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewTeamApplication(Long applicationId, Long reviewerId, boolean approved, String comment) {
        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("团队申请不存在");
        }
        
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("申请已处理");
        }
        
        // 验证审核人是项目创建者
        Project project = projectMapper.selectById(application.getProjectId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        if (!project.getCreatorId().equals(reviewerId)) {
            throw new BusinessException("只有项目创建者可以审核申请");
        }
        
        // 检查是否所有成员都已确认
        LambdaQueryWrapper<TeamApplicationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamApplicationMember::getTeamApplicationId, applicationId);
        List<TeamApplicationMember> members = memberMapper.selectList(wrapper);
        
        long unconfirmedCount = members.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getConfirmed()))
                .count();
        
        if (unconfirmedCount > 0) {
            throw new BusinessException("还有 " + unconfirmedCount + " 位成员未确认，无法审核");
        }
        
        // 更新申请状态
        application.setStatus(approved ? "APPROVED" : "REJECTED");
        application.setReviewedBy(reviewerId);
        application.setReviewComment(comment);
        application.setReviewedAt(LocalDateTime.now());
        teamApplicationMapper.updateById(application);
        
        log.info("审核团队申请: applicationId={}, reviewerId={}, approved={}", 
                applicationId, reviewerId, approved);
        
        // 如果审核通过，将所有确认的成员加入项目团队
        if (approved) {
            // 检查项目是否有关联的团队
            if (project.getTeamId() == null) {
                log.warn("项目 {} 没有关联团队，无法添加成员", project.getId());
                throw new BusinessException("项目尚未关联团队，无法添加成员");
            }
            
            // 将所有确认的成员加入团队
            int addedCount = 0;
            for (TeamApplicationMember member : members) {
                if (Boolean.TRUE.equals(member.getConfirmed())) {
                    try {
                        teamService.addMember(project.getTeamId(), member.getUserId());
                        addedCount++;
                        log.info("成员 {} 已加入项目团队 {}", member.getUserId(), project.getTeamId());
                    } catch (Exception e) {
                        // 如果成员已经在团队中，忽略错误
                        log.warn("添加成员 {} 到团队 {} 失败: {}", 
                                member.getUserId(), project.getTeamId(), e.getMessage());
                    }
                }
            }
            
            log.info("团队申请审核通过，共添加 {} 位成员到项目团队", addedCount);
        }
        
        // 发送通知给所有成员
        String statusText = approved ? "已通过" : "已拒绝";
        String notificationType = approved ? "TEAM_APPLICATION_APPROVED" : "TEAM_APPLICATION_REJECTED";
        
        for (TeamApplicationMember member : members) {
            if (Boolean.TRUE.equals(member.getConfirmed())) {
                String notificationContent = approved 
                    ? String.format("你的团队申请已通过审核，已成功加入项目「%s」", project.getTitle())
                    : String.format("你的团队申请未通过审核。%s", comment != null ? "原因：" + comment : "");
                
                notificationService.createNotification(
                    member.getUserId(),
                    notificationType,
                    "团队申请" + statusText,
                    notificationContent,
                    "TEAM_APPLICATION",
                    applicationId
                );
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTeamApplication(Long applicationId, Long userId) {
        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("团队申请不存在");
        }
        
        if (!application.getLeaderId().equals(userId)) {
            throw new BusinessException("只有发起人可以取消申请");
        }
        
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("申请已处理，无法取消");
        }
        
        application.setStatus("CANCELLED");
        teamApplicationMapper.updateById(application);
        
        log.info("取消团队申请: applicationId={}, userId={}", applicationId, userId);
        
        // 发送通知给所有已确认的成员（除了发起人）
        LambdaQueryWrapper<TeamApplicationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamApplicationMember::getTeamApplicationId, applicationId)
                .eq(TeamApplicationMember::getConfirmed, true)
                .ne(TeamApplicationMember::getUserId, userId);
        
        List<TeamApplicationMember> confirmedMembers = memberMapper.selectList(wrapper);
        Project project = projectMapper.selectById(application.getProjectId());
        
        for (TeamApplicationMember member : confirmedMembers) {
            notificationService.createNotification(
                member.getUserId(),
                "TEAM_APPLICATION_CANCELLED",
                "团队申请已取消",
                String.format("发起人已取消项目「%s」的团队申请", project != null ? project.getTitle() : ""),
                "TEAM_APPLICATION",
                applicationId
            );
        }
    }
    
    /**
     * 转换为DTO
     */
    private TeamApplicationDTO convertToDTO(TeamApplication application) {
        TeamApplicationDTO dto = new TeamApplicationDTO();
        dto.setId(application.getId());
        dto.setProjectId(application.getProjectId());
        dto.setLeaderId(application.getLeaderId());
        dto.setMessage(application.getMessage());
        dto.setStatus(application.getStatus());
        dto.setReviewedBy(application.getReviewedBy());
        dto.setReviewComment(application.getReviewComment());
        dto.setAppliedAt(application.getAppliedAt());
        dto.setReviewedAt(application.getReviewedAt());
        
        // 查询项目信息
        Project project = projectMapper.selectById(application.getProjectId());
        if (project != null) {
            dto.setProjectTitle(project.getTitle());
        }
        
        // 查询发起人信息
        User leader = userMapper.selectById(application.getLeaderId());
        if (leader != null) {
            dto.setLeaderName(leader.getNickname() != null ? leader.getNickname() : leader.getUsername());
        }
        
        // 查询审核人信息
        if (application.getReviewedBy() != null) {
            User reviewer = userMapper.selectById(application.getReviewedBy());
            if (reviewer != null) {
                dto.setReviewerName(reviewer.getNickname() != null ? reviewer.getNickname() : reviewer.getUsername());
            }
        }
        
        // 查询成员列表
        LambdaQueryWrapper<TeamApplicationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamApplicationMember::getTeamApplicationId, application.getId());
        List<TeamApplicationMember> members = memberMapper.selectList(wrapper);
        
        // 批量查询用户信息
        List<Long> userIds = members.stream()
                .map(TeamApplicationMember::getUserId)
                .collect(Collectors.toList());
        
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        List<TeamApplicationDTO.TeamMemberDTO> memberDTOs = members.stream()
                .map(member -> {
                    TeamApplicationDTO.TeamMemberDTO memberDTO = new TeamApplicationDTO.TeamMemberDTO();
                    memberDTO.setUserId(member.getUserId());
                    memberDTO.setConfirmed(member.getConfirmed());
                    memberDTO.setConfirmedAt(member.getConfirmedAt());
                    
                    User user = userMap.get(member.getUserId());
                    if (user != null) {
                        memberDTO.setUsername(user.getUsername());
                        memberDTO.setNickname(user.getNickname());
                        memberDTO.setAvatar(user.getAvatar());
                    }
                    
                    return memberDTO;
                })
                .collect(Collectors.toList());
        
        dto.setMembers(memberDTOs);
        
        return dto;
    }
}
