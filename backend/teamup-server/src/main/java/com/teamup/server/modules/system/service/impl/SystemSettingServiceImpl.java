package com.teamup.server.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamup.server.modules.system.entity.SystemSetting;
import com.teamup.server.modules.system.mapper.SystemSettingMapper;
import com.teamup.server.modules.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {
    
    private final SystemSettingMapper mapper;
    private final ObjectMapper objectMapper;
    
    @Override
    public Map<String, Map<String, Object>> getAllSettings() {
        List<SystemSetting> all = mapper.selectList(null);
        Map<String, Map<String, Object>> result = new HashMap<>();
        
        for (SystemSetting setting : all) {
            String group = setting.getSettingGroup();
            if (!result.containsKey(group)) {
                result.put(group, new HashMap<>());
            }
            
            Object value = parseValue(setting.getSettingValue());
            result.get(group).put(setting.getSettingKey(), value);
        }
        
        // 如果没有数据，返回默认值
        if (result.isEmpty()) {
            result.put("basic", getDefaultBasicSettings());
            result.put("notification", getDefaultNotificationSettings());
            result.put("security", getDefaultSecuritySettings());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> getSettingsByGroup(String group) {
        LambdaQueryWrapper<SystemSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemSetting::getSettingGroup, group);
        
        List<SystemSetting> settings = mapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        
        for (SystemSetting setting : settings) {
            Object value = parseValue(setting.getSettingValue());
            result.put(setting.getSettingKey(), value);
        }
        
        // 如果没有数据，返回默认值
        if (result.isEmpty()) {
            switch (group) {
                case "basic":
                    return getDefaultBasicSettings();
                case "notification":
                    return getDefaultNotificationSettings();
                case "security":
                    return getDefaultSecuritySettings();
                default:
                    return result;
            }
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public void saveSettings(String group, Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = serializeValue(entry.getValue());
            
            // 查找是否已存在
            LambdaQueryWrapper<SystemSetting> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemSetting::getSettingKey, key)
                   .eq(SystemSetting::getSettingGroup, group);
            
            SystemSetting existing = mapper.selectOne(wrapper);
            
            if (existing != null) {
                // 更新
                existing.setSettingValue(value);
                mapper.updateById(existing);
            } else {
                // 新增
                SystemSetting newSetting = new SystemSetting();
                newSetting.setSettingKey(key);
                newSetting.setSettingValue(value);
                newSetting.setSettingGroup(group);
                mapper.insert(newSetting);
            }
        }
    }
    
    @Override
    public String getSetting(String key, String defaultValue) {
        LambdaQueryWrapper<SystemSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemSetting::getSettingKey, key);
        
        SystemSetting setting = mapper.selectOne(wrapper);
        return setting != null ? setting.getSettingValue() : defaultValue;
    }
    
    @Override
    @Transactional
    public void saveSetting(String key, String value, String group) {
        LambdaQueryWrapper<SystemSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemSetting::getSettingKey, key);
        
        SystemSetting existing = mapper.selectOne(wrapper);
        
        if (existing != null) {
            existing.setSettingValue(value);
            existing.setSettingGroup(group);
            mapper.updateById(existing);
        } else {
            SystemSetting newSetting = new SystemSetting();
            newSetting.setSettingKey(key);
            newSetting.setSettingValue(value);
            newSetting.setSettingGroup(group);
            mapper.insert(newSetting);
        }
    }
    
    private Object parseValue(String value) {
        if (value == null) {
            return null;
        }
        
        // 尝试解析为 JSON
        try {
            // 如果是布尔值
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }
            
            // 如果是数字
            if (value.matches("-?\\d+")) {
                return Integer.parseInt(value);
            }
            
            // 如果是 JSON 数组或对象
            if (value.startsWith("[") || value.startsWith("{")) {
                return objectMapper.readValue(value, new TypeReference<Object>() {});
            }
            
            // 否则返回字符串
            return value;
        } catch (Exception e) {
            log.warn("Failed to parse value: {}", value, e);
            return value;
        }
    }
    
    private String serializeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            return (String) value;
        }
        
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("Failed to serialize value: {}", value, e);
            return value.toString();
        }
    }
    
    private Map<String, Object> getDefaultBasicSettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("siteName", "Team Up");
        defaults.put("siteDescription", "团队协作平台");
        defaults.put("maintenanceMode", false);
        defaults.put("allowRegistration", true);
        defaults.put("requireRegistrationApproval", false);
        return defaults;
    }
    
    private Map<String, Object> getDefaultNotificationSettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("emailEnabled", false);
        defaults.put("smtpHost", "");
        defaults.put("smtpPort", 587);
        defaults.put("senderEmail", "");
        return defaults;
    }
    
    private Map<String, Object> getDefaultSecuritySettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("minPasswordLength", 6);
        defaults.put("passwordRequirements", List.of("lowercase", "number"));
        defaults.put("loginLockEnabled", false);
        defaults.put("maxLoginAttempts", 5);
        defaults.put("lockoutDuration", 15);
        return defaults;
    }
}
