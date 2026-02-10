package com.teamup.server.modules.tag.vo;

import lombok.Data;

/**
 * 用户技能VO
 */
@Data
public class UserSkillVO {
    
    /**
     * 用户标签关联ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 标签ID
     */
    private Long tagId;
    
    /**
     * 标签名称
     */
    private String tagName;
    
    /**
     * 技能名称（兼容旧字段）
     */
    private String skillName;
    
    /**
     * 熟练度
     */
    private String proficiencyLevel;
    
    /**
     * 是否已认证
     */
    private Boolean isVerified;
    
    /**
     * 设置标签名称时同时设置技能名称（保持兼容性）
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
        this.skillName = tagName;
    }
}
