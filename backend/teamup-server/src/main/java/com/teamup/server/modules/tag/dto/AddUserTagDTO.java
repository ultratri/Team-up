package com.teamup.server.modules.tag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加用户标签DTO（通用）
 */
@Data
public class AddUserTagDTO {
    
    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空")
    private Long tagId;
    
    /**
     * 熟练度（仅技能标签使用）：BEGINNER/INTERMEDIATE/ADVANCED/EXPERT
     */
    private String proficiencyLevel;
}
