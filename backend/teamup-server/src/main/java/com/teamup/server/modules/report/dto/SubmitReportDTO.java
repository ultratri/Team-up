package com.teamup.server.modules.report.dto;

import com.teamup.server.modules.report.entity.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交举报DTO
 */
@Data
public class SubmitReportDTO {
    
    @NotNull(message = "举报目标类型不能为空")
    private Report.TargetType targetType;
    
    @NotNull(message = "举报目标ID不能为空")
    private Long targetId;
    
    @NotNull(message = "举报原因不能为空")
    private Report.ReportReason reason;
    
    @NotBlank(message = "详细描述不能为空")
    private String description;
    
    /**
     * 证据链接列表
     */
    private List<String> evidenceUrls;
}
