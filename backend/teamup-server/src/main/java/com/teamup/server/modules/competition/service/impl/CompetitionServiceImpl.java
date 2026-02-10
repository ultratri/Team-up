package com.teamup.server.modules.competition.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.mapper.CompetitionMapper;
import com.teamup.server.modules.competition.service.CompetitionService;
import org.springframework.stereotype.Service;

/**
 * 比赛服务实现
 */
@Service
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition> implements CompetitionService {
}

