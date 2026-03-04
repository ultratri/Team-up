package com.teamup.server.modules.project.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 团队申请请求DTO
 */
@Data
public class TeamApplicationRequest {
    
    /**
     * 申请人ID列表（包含发起人）
     */
    @NotEmpty(message = "申请人列表不能为空")
    private List<Long> applicantIds;
    
    /**
     * 申请说明
     */
    private String message;
}
