package com.teamup.server.modules.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报实体
 */
@Data
@TableName("reports")
public class Report {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 举报人ID
     */
    private Long reporterId;
    
    /**
     * 举报目标类型
     */
    private TargetType targetType;
    
    /**
     * 举报目标ID
     */
    private Long targetId;
    
    /**
     * 举报原因
     */
    private ReportReason reason;
    
    /**
     * 详细描述
     */
    private String description;
    
    /**
     * 证据链接（JSON格式）
     */
    private String evidenceUrls;
    
    /**
     * 处理状态
     */
    private ReportStatus status;
    
    /**
     * 处理人ID
     */
    private Long handlerId;
    
    /**
     * 处理结果说明
     */
    private String handleResult;
    
    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 举报目标类型枚举
     */
    public enum TargetType {
        PROJECT,    // 项目
        TEAM,       // 团队
        USER,       // 用户
        COMMENT     // 评论
    }
    
    /**
     * 举报原因枚举
     */
    public enum ReportReason {
        SPAM,           // 垃圾信息
        FRAUD,          // 诈骗
        INAPPROPRIATE,  // 不当内容
        HARASSMENT,     // 骚扰
        OTHER           // 其他
    }
    
    /**
     * 处理状态枚举
     */
    public enum ReportStatus {
        PENDING,    // 待处理
        REVIEWING,  // 审核中
        RESOLVED,   // 已处理
        REJECTED    // 已驳回
    }
}
