package com.teamup.server.modules.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.notification.entity.Notification;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {
    
    /**
     * 创建通知
     * 
     * @param userId 用户ID
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param relatedType 关联类型
     * @param relatedId 关联ID
     */
    void createNotification(Long userId, String type, String title, String content, String relatedType, Long relatedId);
    
    /**
     * 批量创建通知
     * 
     * @param userIds 用户ID列表
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param relatedType 关联类型
     * @param relatedId 关联ID
     */
    void createBatchNotifications(List<Long> userIds, String type, String title, String content, String relatedType, Long relatedId);
    
    /**
     * 获取用户通知列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @param isRead 是否已读（null表示全部）
     * @param type 通知类型（null表示全部）
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 通知分页列表
     */
    Page<Notification> getUserNotifications(Long userId, int page, int size, Boolean isRead, String type, String sortBy, String sortOrder);
    
    /**
     * 获取未读通知数量
     * 
     * @param userId 用户ID
     * @return 未读数量
     */
    Long getUnreadCount(Long userId);
    
    /**
     * 标记通知为已读
     * 
     * @param notificationId 通知ID
     * @param userId 用户ID
     */
    void markAsRead(Long notificationId, Long userId);
    
    /**
     * 标记所有通知为已读
     * 
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);
    
    /**
     * 删除通知
     * 
     * @param notificationId 通知ID
     * @param userId 用户ID
     */
    void deleteNotification(Long notificationId, Long userId);
}
