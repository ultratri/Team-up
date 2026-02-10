package com.teamup.server.modules.tag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加用户技能DTO
 */
@Data
public class AddUserSkillDTO {
    
    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空")
    private Long tagId;
    
    /**
     * 熟练度：BEGINNER/INTERMEDIATE/ADVANCED/EXPERT
     */
    @NotNull(message = "熟练度不能为空")
    private String proficiencyLevel;
}
