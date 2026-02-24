package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.util.List;

/**
 * 团队自定义配置 VO
 */
@Data
public class TeamCustomConfigVO {
    private Long id;
    private Long teamId;
    
    // 自定义快捷入口
    private List<ShortcutVO> shortcuts;
    
    // 自定义分组
    private List<ToolGroupVO> groups;
    
    // 团队首页信息
    private String teamAnnouncement;
    private List<GuidelineVO> teamGuidelines;
    private List<ChecklistItemVO> onboardingChecklist;
    
    // 权限配置
    private String shortcutsEditPermission;
    private String announcementEditPermission;
    
    // 辅助字段
    private Boolean canEditShortcuts;
    private Boolean canEditAnnouncement;
}
