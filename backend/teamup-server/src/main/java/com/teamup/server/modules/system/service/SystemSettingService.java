package com.teamup.server.modules.system.service;

import java.util.Map;

public interface SystemSettingService {
    
    /**
     * 获取所有设置（按分组）
     */
    Map<String, Map<String, Object>> getAllSettings();
    
    /**
     * 获取指定分组的设置
     */
    Map<String, Object> getSettingsByGroup(String group);
    
    /**
     * 保存设置（批量）
     */
    void saveSettings(String group, Map<String, Object> settings);
    
    /**
     * 获取单个设置值
     */
    String getSetting(String key, String defaultValue);
    
    /**
     * 保存单个设置
     */
    void saveSetting(String key, String value, String group);
}
