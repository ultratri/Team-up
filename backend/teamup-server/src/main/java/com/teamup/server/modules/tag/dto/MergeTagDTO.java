package com.teamup.server.modules.tag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合并标签DTO
 */
@Data
public class MergeTagDTO {
    
    @NotNull(message = "源标签ID不能为空")
    private Long sourceTagId;
    
    @NotNull(message = "目标标签ID不能为空")
    private Long targetTagId;
}
