package com.teamup.server.modules.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.notification.entity.Notification;
import com.teamup.server.modules.notification.mapper.NotificationMapper;
import com.teamup.server.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationMapper notificationMapper;
    
    @Override
    public void createNotification(Long userId, String type, String title, String content, String relatedType, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationMapper.insert(notification);
        log.info("通知创建成功: userId={}, type={}, title={}", userId, type, title);
    }
    
    @Override
    public void createBatchNotifications(List<Long> userIds, String type, String title, String content, String relatedType, Long relatedId) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("批量创建通知失败: 用户ID列表为空");
            return;
        }
        
        for (Long userId : userIds) {
            try {
                createNotification(userId, type, title, content, relatedType, relatedId);
            } catch (Exception e) {
                log.error("创建通知失败: userId={}, type={}", userId, type, e);
            }
        }
        
        log.info("批量通知创建完成: count={}, type={}", userIds.size(), type);
    }
    
    @Override
    public Page<Notification> getUserNotifications(Long userId, int page, int size, Boolean isRead, String type, String sortBy, String sortOrder) {
        Page<Notification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(Notification::getUserId, userId);
        
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notification::getType, type);
        }
        
        // 排序
        if ("desc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByDesc(Notification::getCreatedAt);
        } else {
            wrapper.orderByAsc(Notification::getCreatedAt);
        }
        
        return notificationMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false);
        
        return notificationMapper.selectCount(wrapper);
    }
    
    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此通知");
        }
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
            log.info("通知已标记为已读: notificationId={}, userId={}", notificationId, userId);
        }
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false);
        
        List<Notification> unreadNotifications = notificationMapper.selectList(wrapper);
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
        
        log.info("所有通知已标记为已读: userId={}, count={}", userId, unreadNotifications.size());
    }
    
    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此通知");
        }
        
        notificationMapper.deleteById(notificationId);
        log.info("通知已删除: notificationId={}, userId={}", notificationId, userId);
    }
}
