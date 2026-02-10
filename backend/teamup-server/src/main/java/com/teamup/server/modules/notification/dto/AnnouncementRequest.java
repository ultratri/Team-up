package com.teamup.server.modules.notification.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 公告请求DTO
 */
@Data
public class AnnouncementRequest {
    @NotBlank(message = "公告标题不能为空")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    /**
     * 接收者类型：ALL（所有用户）、SPECIFIC（指定用户）
     */
    @NotBlank(message = "接收者类型不能为空")
    private String receiverType;

    /**
     * 指定用户ID列表（当receiverType为SPECIFIC时使用）
     */
    private List<Long> userIds;

    /**
     * 公告优先级：HIGH（高）、MEDIUM（中）、LOW（低）
     */
    private String priority = "MEDIUM";

    /**
     * 通知类型：用于通知中心的类型筛选
     * 如：SYSTEM_ANNOUNCEMENT、COMPETITION_ANNOUNCEMENT、PROJECT_ANNOUNCEMENT等
     */
    private String notificationType = "SYSTEM_ANNOUNCEMENT";
}
