package com.teamup.server.modules.project.service;

import com.teamup.server.modules.project.vo.MilestoneVO;

import java.util.List;

public interface ProjectMilestoneService {

    List<MilestoneVO> listByProject(Long projectId, Long userId);

    MilestoneVO createMilestone(Long projectId, Long userId, MilestoneVO payload);

    MilestoneVO updateMilestone(Long milestoneId, Long userId, MilestoneVO payload);

    void deleteMilestone(Long milestoneId, Long userId);
}
