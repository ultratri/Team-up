package com.teamup.server.modules.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.notification.entity.Notification;

/**
 * 通知服务接口
 */
public interface NotificationService {
    /**
     * 创建通知
     */
    void createNotification(Long userId, String type, String title, String content, 
                           String relatedType, Long relatedId);
    
    /**
     * 批量创建通知
     */
    void createBatchNotifications(java.util.List<Long> userIds, String type, String title, 
                                  String content, String relatedType, Long relatedId);
    
    /**
     * 获取用户通知列表
     */
    Page<Notification> getUserNotifications(Long userId, int page, int size, Boolean isRead, String type, 
                                           String sortBy, String sortOrder);
    
    /**
     * 标记通知为已读
     */
    void markAsRead(Long notificationId, Long userId);
    
    /**
     * 全部标记为已读
     */
    void markAllAsRead(Long userId);
    
    /**
     * 删除通知
     */
    void deleteNotification(Long notificationId, Long userId);
    
    /**
     * 获取未读数量
     */
    Long getUnreadCount(Long userId);
}
