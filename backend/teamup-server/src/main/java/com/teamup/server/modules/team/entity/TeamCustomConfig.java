package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队自定义配置实体类
 * 用于存储团队的高级自定义配置，如快捷入口、分组、公告等
 */
@Data
@TableName("team_custom_config")
public class TeamCustomConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    
    // JSON 字段
    private String shortcutsJson;
    private String groupsJson;
    
    // 团队首页信息
    private String teamAnnouncement;
    private String teamGuidelinesJson;
    private String onboardingChecklistJson;
    
    // 权限配置
    private String shortcutsEditPermission;
    private String announcementEditPermission;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
