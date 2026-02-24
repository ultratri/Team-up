package com.teamup.server.modules.team.service;

import com.teamup.server.modules.team.entity.TeamSetting;
import java.util.Map;

/**
 * 团队设置服务接口
 */
public interface TeamSettingService {
    
    /**
     * 保存团队设置
     */
    void saveSetting(Long teamId, String key, String value);
    
    /**
     * 获取团队设置
     */
    String getSetting(Long teamId, String key);
    
    /**
     * 获取团队所有设置
     */
    Map<String, String> getAllSettings(Long teamId);
    
    /**
     * 删除团队设置
     */
    void deleteSetting(Long teamId, String key);
    
    /**
     * 批量保存团队设置
     */
    void batchSaveSettings(Long teamId, Map<String, String> settings);
}
