package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamup.server.common.exception.AuthorizationException;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.team.entity.TeamCustomConfig;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamCustomConfigMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.service.TeamCustomConfigService;
import com.teamup.server.modules.team.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 团队自定义配置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCustomConfigServiceImpl implements TeamCustomConfigService {
    
    private final TeamCustomConfigMapper configMapper;
    private final TeamMemberMapper memberMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    public TeamCustomConfigVO getConfig(Long teamId, Long currentUserId) {
        // 查询配置
        LambdaQueryWrapper<TeamCustomConfig> query = new LambdaQueryWrapper<>();
        query.eq(TeamCustomConfig::getTeamId, teamId);
        TeamCustomConfig config = configMapper.selectOne(query);
        
        if (config == null) {
            // 如果不存在，初始化默认配置
            initConfig(teamId);
            config = configMapper.selectOne(query);
        }
        
        // 转换为 VO
        TeamCustomConfigVO vo = new TeamCustomConfigVO();
        vo.setId(config.getId());
        vo.setTeamId(config.getTeamId());
        vo.setTeamAnnouncement(config.getTeamAnnouncement());
        vo.setShortcutsEditPermission(config.getShortcutsEditPermission());
        vo.setAnnouncementEditPermission(config.getAnnouncementEditPermission());
        
        // 解析 JSON 字段
        try {
            if (config.getShortcutsJson() != null) {
                vo.setShortcuts(objectMapper.readValue(
                    config.getShortcutsJson(), 
                    new TypeReference<List<ShortcutVO>>() {}
                ));
            } else {
                vo.setShortcuts(new ArrayList<>());
            }
            
            if (config.getGroupsJson() != null) {
                vo.setGroups(objectMapper.readValue(
                    config.getGroupsJson(), 
                    new TypeReference<List<ToolGroupVO>>() {}
                ));
            } else {
                vo.setGroups(getDefaultGroups());
            }
            
            if (config.getTeamGuidelinesJson() != null) {
                vo.setTeamGuidelines(objectMapper.readValue(
                    config.getTeamGuidelinesJson(), 
                    new TypeReference<List<GuidelineVO>>() {}
                ));
            } else {
                vo.setTeamGuidelines(new ArrayList<>());
            }
            
            if (config.getOnboardingChecklistJson() != null) {
                vo.setOnboardingChecklist(objectMapper.readValue(
                    config.getOnboardingChecklistJson(), 
                    new TypeReference<List<ChecklistItemVO>>() {}
                ));
            } else {
                vo.setOnboardingChecklist(new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("解析团队配置 JSON 失败", e);
            throw new BusinessException("配置数据格式错误");
        }
        
        // 判断当前用户权限
        boolean isLeader = isTeamLeader(teamId, currentUserId);
        vo.setCanEditShortcuts(canEdit(config.getShortcutsEditPermission(), isLeader));
        vo.setCanEditAnnouncement(canEdit(config.getAnnouncementEditPermission(), isLeader));
        
        return vo;
    }
    
    @Override
    @Transactional
    public void updateConfig(Long teamId, Long currentUserId, TeamCustomConfigVO configVO) {
        // 查询现有配置
        LambdaQueryWrapper<TeamCustomConfig> query = new LambdaQueryWrapper<>();
        query.eq(TeamCustomConfig::getTeamId, teamId);
        TeamCustomConfig config = configMapper.selectOne(query);
        
        if (config == null) {
            throw new BusinessException("团队配置不存在");
        }
        
        // 权限检查
        boolean isLeader = isTeamLeader(teamId, currentUserId);
        
        // 转换 JSON 字段
        try {
            // 快捷入口
            if (configVO.getShortcuts() != null) {
                if (!canEdit(config.getShortcutsEditPermission(), isLeader)) {
                    throw new AuthorizationException("无权限编辑快捷入口");
                }
                config.setShortcutsJson(objectMapper.writeValueAsString(configVO.getShortcuts()));
            }
            
            // 分组
            if (configVO.getGroups() != null) {
                if (!canEdit(config.getShortcutsEditPermission(), isLeader)) {
                    throw new AuthorizationException("无权限编辑工具分组");
                }
                config.setGroupsJson(objectMapper.writeValueAsString(configVO.getGroups()));
            }
            
            // 团队公告
            if (configVO.getTeamAnnouncement() != null) {
                if (!canEdit(config.getAnnouncementEditPermission(), isLeader)) {
                    throw new AuthorizationException("无权限编辑团队公告");
                }
                config.setTeamAnnouncement(configVO.getTeamAnnouncement());
            }
            
            // 规范链接
            if (configVO.getTeamGuidelines() != null) {
                if (!canEdit(config.getAnnouncementEditPermission(), isLeader)) {
                    throw new AuthorizationException("无权限编辑规范链接");
                }
                config.setTeamGuidelinesJson(objectMapper.writeValueAsString(configVO.getTeamGuidelines()));
            }
            
            // 新人指引
            if (configVO.getOnboardingChecklist() != null) {
                if (!canEdit(config.getAnnouncementEditPermission(), isLeader)) {
                    throw new AuthorizationException("无权限编辑新人指引");
                }
                config.setOnboardingChecklistJson(objectMapper.writeValueAsString(configVO.getOnboardingChecklist()));
            }
            
            // 权限配置（仅队长可修改）
            if (configVO.getShortcutsEditPermission() != null && isLeader) {
                config.setShortcutsEditPermission(configVO.getShortcutsEditPermission());
            }
            if (configVO.getAnnouncementEditPermission() != null && isLeader) {
                config.setAnnouncementEditPermission(configVO.getAnnouncementEditPermission());
            }
            
        } catch (AuthorizationException e) {
            throw e;
        } catch (Exception e) {
            log.error("序列化团队配置 JSON 失败", e);
            throw new BusinessException("配置数据格式错误");
        }
        
        configMapper.updateById(config);
    }
    
    @Override
    @Transactional
    public void initConfig(Long teamId) {
        // 检查是否已存在
        LambdaQueryWrapper<TeamCustomConfig> query = new LambdaQueryWrapper<>();
        query.eq(TeamCustomConfig::getTeamId, teamId);
        if (configMapper.selectOne(query) != null) {
            return;
        }
        
        // 创建默认配置
        TeamCustomConfig config = new TeamCustomConfig();
        config.setTeamId(teamId);
        config.setShortcutsEditPermission("leader");
        config.setAnnouncementEditPermission("leader");
        
        try {
            config.setShortcutsJson(objectMapper.writeValueAsString(new ArrayList<>()));
            config.setGroupsJson(objectMapper.writeValueAsString(getDefaultGroups()));
            config.setTeamGuidelinesJson(objectMapper.writeValueAsString(new ArrayList<>()));
            config.setOnboardingChecklistJson(objectMapper.writeValueAsString(new ArrayList<>()));
        } catch (Exception e) {
            log.error("初始化团队配置失败", e);
            throw new BusinessException("初始化配置失败");
        }
        
        configMapper.insert(config);
    }
    
    /**
     * 判断用户是否为团队队长
     */
    private boolean isTeamLeader(Long teamId, Long userId) {
        LambdaQueryWrapper<TeamMember> query = new LambdaQueryWrapper<>();
        query.eq(TeamMember::getTeamId, teamId)
             .eq(TeamMember::getUserId, userId)
             .eq(TeamMember::getRole, "LEADER");
        return memberMapper.selectCount(query) > 0;
    }
    
    /**
     * 判断是否有编辑权限
     */
    private boolean canEdit(String permission, boolean isLeader) {
        if (isLeader) {
            return true;
        }
        return "all".equals(permission);
    }
    
    /**
     * 获取默认分组
     */
    private List<ToolGroupVO> getDefaultGroups() {
        List<ToolGroupVO> groups = new ArrayList<>();
        
        ToolGroupVO dev = new ToolGroupVO();
        dev.setId("dev");
        dev.setName("研发工具");
        dev.setOrder(1);
        dev.setLinks(new ArrayList<>());
        groups.add(dev);
        
        ToolGroupVO collab = new ToolGroupVO();
        collab.setId("collab");
        collab.setName("协作工具");
        collab.setOrder(2);
        collab.setLinks(new ArrayList<>());
        groups.add(collab);
        
        ToolGroupVO design = new ToolGroupVO();
        design.setId("design");
        design.setName("设计工具");
        design.setOrder(3);
        design.setLinks(new ArrayList<>());
        groups.add(design);
        
        return groups;
    }
}
