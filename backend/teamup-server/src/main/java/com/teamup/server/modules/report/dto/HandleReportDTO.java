package com.teamup.server.modules.report.dto;

import com.teamup.server.modules.report.entity.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理举报DTO
 */
@Data
public class HandleReportDTO {
    
    @NotNull(message = "举报ID不能为空")
    private Long reportId;
    
    @NotNull(message = "处理状态不能为空")
    private Report.ReportStatus status;
    
    @NotBlank(message = "处理结果不能为空")
    private String handleResult;
    
    /**
     * 是否对目标进行惩罚
     */
    private Boolean punishTarget;
    
    /**
     * 惩罚类型：BAN_USER/DELETE_CONTENT/DEDUCT_CREDIT
     */
    private String punishmentType;
    
    /**
     * 惩罚时长（天数，仅对封禁有效）
     */
    private Integer punishmentDays;
}
