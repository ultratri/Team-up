package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.teamup.server.modules.team.entity.TeamJoinApplication;

public interface TeamJoinApplicationService extends IService<TeamJoinApplication> {
    TeamJoinApplication apply(Long teamId, Long applicantId, String reason);

    Page<TeamJoinApplication> listForTeam(Long teamId, Long operatorId, int page, int size, String status);

    Page<TeamJoinApplication> listMy(Long userId, int page, int size, String status);

    void review(Long applicationId, Long operatorId, boolean approved, String comment);

    void withdraw(Long applicationId, Long applicantId);
}

