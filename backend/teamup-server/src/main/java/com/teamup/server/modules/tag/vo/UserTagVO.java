package com.teamup.server.modules.tag.vo;

import lombok.Data;

/**
 * 用户标签VO（通用）
 */
@Data
public class UserTagVO {
    
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
     * 标签类型/分类（SKILL, INTEREST, PERSONALITY, PROJECT_TYPE等）
     */
    private String tagType;
    
    /**
     * 熟练度（仅技能标签）
     */
    private String proficiencyLevel;
    
    /**
     * 是否已认证
     */
    private Boolean isVerified;
}
