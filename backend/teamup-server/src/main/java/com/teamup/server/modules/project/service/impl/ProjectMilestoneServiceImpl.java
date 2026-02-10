package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.teamup.server.common.api.ApiErrorCode;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectMilestone;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.project.mapper.ProjectMilestoneMapper;
import com.teamup.server.modules.project.service.ProjectMilestoneService;
import com.teamup.server.modules.project.vo.MilestoneVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {

    private static final Set<String> ALLOWED_STATUS = new HashSet<>(List.of("PLANNED", "IN_PROGRESS", "DONE"));

    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

    @Override
    public List<MilestoneVO> listByProject(Long projectId, Long userId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        // 允许项目成员/创建者查看，这里只校验存在

        LambdaQueryWrapper<ProjectMilestone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectMilestone::getProjectId, projectId)
                .orderByAsc(ProjectMilestone::getSortOrder)
                .orderByAsc(ProjectMilestone::getPlannedAt)
                .orderByAsc(ProjectMilestone::getId);

        List<ProjectMilestone> list = milestoneMapper.selectList(wrapper);
        return convertListToVO(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MilestoneVO createMilestone(Long projectId, Long userId, MilestoneVO payload) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        ensureProjectOwner(project, userId);

        String status = StringUtils.hasText(payload.getStatus()) ? payload.getStatus() : "PLANNED";
        validateStatus(status);

        ProjectMilestone milestone = new ProjectMilestone();
        milestone.setProjectId(projectId);
        milestone.setTitle(payload.getTitle());
        milestone.setStatus(status);
        milestone.setPlannedAt(payload.getPlannedAt());
        milestone.setActualAt(payload.getActualAt());
        milestone.setOwnerId(payload.getOwnerId());
        milestone.setRemark(payload.getRemark());
        milestone.setSortOrder(payload.getSortOrder() == null ? 0 : payload.getSortOrder());
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());

        milestoneMapper.insert(milestone);
        return convertToVO(milestone, fetchUserNames(List.of(milestone.getOwnerId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MilestoneVO updateMilestone(Long milestoneId, Long userId, MilestoneVO payload) {
        ProjectMilestone existing = milestoneMapper.selectById(milestoneId);
        if (existing == null) {
            throw new BusinessException("里程碑不存在");
        }

        Project project = projectMapper.selectById(existing.getProjectId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        ensureProjectOwner(project, userId);

        String status = payload.getStatus();
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }

        LambdaUpdateWrapper<ProjectMilestone> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProjectMilestone::getId, milestoneId);

        if (StringUtils.hasText(payload.getTitle())) {
            wrapper.set(ProjectMilestone::getTitle, payload.getTitle());
        }
        if (StringUtils.hasText(status)) {
            wrapper.set(ProjectMilestone::getStatus, status);
        }
        if (payload.getPlannedAt() != null) {
            wrapper.set(ProjectMilestone::getPlannedAt, payload.getPlannedAt());
        }
        if (payload.getActualAt() != null) {
            wrapper.set(ProjectMilestone::getActualAt, payload.getActualAt());
        }
        if (payload.getOwnerId() != null) {
            wrapper.set(ProjectMilestone::getOwnerId, payload.getOwnerId());
        }
        if (payload.getRemark() != null) {
            wrapper.set(ProjectMilestone::getRemark, payload.getRemark());
        }
        if (payload.getSortOrder() != null) {
            wrapper.set(ProjectMilestone::getSortOrder, payload.getSortOrder());
        }
        wrapper.set(ProjectMilestone::getUpdatedAt, LocalDateTime.now());

        int updated = milestoneMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BusinessException("更新失败");
        }

        ProjectMilestone latest = milestoneMapper.selectById(milestoneId);
        return convertToVO(latest, fetchUserNames(List.of(latest.getOwnerId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMilestone(Long milestoneId, Long userId) {
        ProjectMilestone existing = milestoneMapper.selectById(milestoneId);
        if (existing == null) {
            throw new BusinessException("里程碑不存在");
        }

        Project project = projectMapper.selectById(existing.getProjectId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        ensureProjectOwner(project, userId);

        milestoneMapper.deleteById(milestoneId);
    }

    private void ensureProjectOwner(Project project, Long userId) {
        if (!project.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "无权操作该项目里程碑");
        }
    }

    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BusinessException("里程碑状态不合法");
        }
    }

    private List<MilestoneVO> convertListToVO(List<ProjectMilestone> list) {
        List<Long> ownerIds = list.stream()
                .map(ProjectMilestone::getOwnerId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, String> userNames = fetchUserNames(ownerIds);
        return list.stream()
                .map(item -> convertToVO(item, userNames))
                .collect(Collectors.toList());
    }

    private MilestoneVO convertToVO(ProjectMilestone milestone, Map<Long, String> userNames) {
        MilestoneVO vo = new MilestoneVO();
        vo.setId(milestone.getId());
        vo.setProjectId(milestone.getProjectId());
        vo.setTitle(milestone.getTitle());
        vo.setStatus(milestone.getStatus());
        vo.setPlannedAt(milestone.getPlannedAt());
        vo.setActualAt(milestone.getActualAt());
        vo.setOwnerId(milestone.getOwnerId());
        vo.setOwnerName(milestone.getOwnerId() != null ? userNames.get(milestone.getOwnerId()) : null);
        vo.setRemark(milestone.getRemark());
        vo.setSortOrder(milestone.getSortOrder());
        vo.setCreatedAt(milestone.getCreatedAt());
        vo.setUpdatedAt(milestone.getUpdatedAt());
        return vo;
    }

    private Map<Long, String> fetchUserNames(List<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(ownerIds);
        Map<Long, String> map = new HashMap<>();
        for (User user : users) {
            map.put(user.getId(), user.getUsername());
        }
        return map;
    }
}
