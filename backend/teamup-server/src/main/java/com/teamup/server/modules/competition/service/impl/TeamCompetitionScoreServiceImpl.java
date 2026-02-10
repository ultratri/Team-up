package com.teamup.server.modules.competition.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.competition.entity.TeamCompetitionScore;
import com.teamup.server.modules.competition.mapper.TeamCompetitionScoreMapper;
import com.teamup.server.modules.competition.service.TeamCompetitionScoreService;
import org.springframework.stereotype.Service;

@Service
public class TeamCompetitionScoreServiceImpl extends ServiceImpl<TeamCompetitionScoreMapper, TeamCompetitionScore>
        implements TeamCompetitionScoreService {
}

