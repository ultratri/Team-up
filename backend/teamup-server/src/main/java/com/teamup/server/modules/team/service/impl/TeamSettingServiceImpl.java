package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.team.entity.TeamSetting;
import com.teamup.server.modules.team.mapper.TeamSettingMapper;
import com.teamup.server.modules.team.service.TeamSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 团队设置服务实现类
 */
@Service
@RequiredArgsConstructor
public class TeamSettingServiceImpl implements TeamSettingService {
    
    private final TeamSettingMapper settingMapper;
    
    @Override
    @Transactional
    public void saveSetting(Long teamId, String key, String value) {
        LambdaQueryWrapper<TeamSetting> query = new LambdaQueryWrapper<>();
        query.eq(TeamSetting::getTeamId, teamId)
             .eq(TeamSetting::getSettingKey, key);
        
        TeamSetting existing = settingMapper.selectOne(query);
        
        if (existing != null) {
            existing.setSettingValue(value);
            settingMapper.updateById(existing);
        } else {
            TeamSetting setting = new TeamSetting();
            setting.setTeamId(teamId);
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            settingMapper.insert(setting);
        }
    }
    
    @Override
    public String getSetting(Long teamId, String key) {
        LambdaQueryWrapper<TeamSetting> query = new LambdaQueryWrapper<>();
        query.eq(TeamSetting::getTeamId, teamId)
             .eq(TeamSetting::getSettingKey, key);
        
        TeamSetting setting = settingMapper.selectOne(query);
        return setting != null ? setting.getSettingValue() : null;
    }
    
    @Override
    public Map<String, String> getAllSettings(Long teamId) {
        LambdaQueryWrapper<TeamSetting> query = new LambdaQueryWrapper<>();
        query.eq(TeamSetting::getTeamId, teamId);
        
        List<TeamSetting> settings = settingMapper.selectList(query);
        
        return settings.stream()
                .collect(Collectors.toMap(
                        TeamSetting::getSettingKey,
                        TeamSetting::getSettingValue
                ));
    }
    
    @Override
    @Transactional
    public void deleteSetting(Long teamId, String key) {
        LambdaQueryWrapper<TeamSetting> query = new LambdaQueryWrapper<>();
        query.eq(TeamSetting::getTeamId, teamId)
             .eq(TeamSetting::getSettingKey, key);
        
        settingMapper.delete(query);
    }
    
    @Override
    @Transactional
    public void batchSaveSettings(Long teamId, Map<String, String> settings) {
        settings.forEach((key, value) -> saveSetting(teamId, key, value));
    }
}
