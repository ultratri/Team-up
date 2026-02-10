package com.teamup.server.modules.report.vo;

import com.teamup.server.modules.report.entity.Report;
import lombok.Data;

/**
 * 举报统计VO
 */
@Data
public class ReportStatisticsVO {
    
    private Report.TargetType targetType;
    private Report.ReportStatus status;
    private Long count;
}
