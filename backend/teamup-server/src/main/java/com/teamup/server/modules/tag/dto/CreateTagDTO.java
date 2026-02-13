package com.teamup.server.modules.tag.dto;

import com.teamup.server.modules.tag.entity.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建标签DTO
 */
@Data
public class CreateTagDTO {
    
    @NotBlank(message = "标签名称不能为空")
    private String name;
    
    @NotNull(message = "标签分类不能为空")
    private String category;
    
    private Long parentId;
    
    private String description;
    
    private Boolean isOfficial;
}
