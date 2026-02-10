package com.teamup.server.modules.competition.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.competition.entity.CompetitionTemplate;
import com.teamup.server.modules.competition.mapper.CompetitionTemplateMapper;
import com.teamup.server.modules.competition.service.CompetitionTemplateService;
import org.springframework.stereotype.Service;

@Service
public class CompetitionTemplateServiceImpl extends ServiceImpl<CompetitionTemplateMapper, CompetitionTemplate>
        implements CompetitionTemplateService {
}

