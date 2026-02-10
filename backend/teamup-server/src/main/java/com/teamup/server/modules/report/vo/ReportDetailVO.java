package com.teamup.server.modules.report.vo;

import com.teamup.server.modules.report.entity.Report;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报详情VO
 */
@Data
public class ReportDetailVO {
    
    private Long id;
    private Long reporterId;
    private String reporterName;
    private Report.TargetType targetType;
    private Long targetId;
    private String targetName;  // 目标名称（项目名/用户名等）
    private Report.ReportReason reason;
    private String description;
    private List<String> evidenceUrls;
    private Report.ReportStatus status;
    private Long handlerId;
    private String handlerName;
    private String handleResult;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
