package com.teamup.server.modules.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.notification.entity.Notification;
import com.teamup.server.modules.notification.mapper.NotificationMapper;
import com.teamup.server.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Long userId, String type, String title, String content,
                                   String relatedType, Long relatedId) {
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
        log.info("创建通知: userId={}, type={}, title={}", userId, type, title);
        
        // TODO: 通过 WebSocket 推送实时通知
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatchNotifications(List<Long> userIds, String type, String title,
                                        String content, String relatedType, Long relatedId) {
        for (Long userId : userIds) {
            createNotification(userId, type, title, content, relatedType, relatedId);
        }
    }

    @Override
    public Page<Notification> getUserNotifications(Long userId, int page, int size, Boolean isRead, String type,
                                                   String sortBy, String sortOrder) {
        Page<Notification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        
        if (type != null && !type.isEmpty()) {
            // 支持前缀匹配和精确匹配
            if (type.equals("COMPETITION")) {
                // 匹配所有比赛相关：COMPETITION_*
                wrapper.like(Notification::getType, "COMPETITION");
            } else if (type.equals("TEAM_JOIN")) {
                // 匹配所有入队申请：TEAM_JOIN_*
                wrapper.like(Notification::getType, "TEAM_JOIN");
            } else if (type.equals("MENTOR_APPLICATION")) {
                // 匹配所有导师申请：MENTOR_APPLICATION_*
                wrapper.like(Notification::getType, "MENTOR_APPLICATION");
            } else if (type.equals("PROJECT")) {
                // 匹配所有项目相关：PROJECT_*
                wrapper.like(Notification::getType, "PROJECT");
            } else if (type.equals("TEAM")) {
                // 匹配所有团队相关：TEAM_*（但排除 TEAM_JOIN）
                wrapper.like(Notification::getType, "TEAM")
                       .notLike(Notification::getType, "TEAM_JOIN");
            } else if (type.equals("SYSTEM")) {
                // 匹配所有系统相关：SYSTEM_*
                wrapper.like(Notification::getType, "SYSTEM");
            } else {
                // 精确匹配（如 SYSTEM_ANNOUNCEMENT）
                wrapper.eq(Notification::getType, type);
            }
        }
        
        // 排序
        if (sortBy != null && !sortBy.isEmpty()) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            switch (sortBy) {
                case "createdAt":
                    if (isAsc) {
                        wrapper.orderByAsc(Notification::getCreatedAt);
                    } else {
                        wrapper.orderByDesc(Notification::getCreatedAt);
                    }
                    break;
                case "type":
                    if (isAsc) {
                        wrapper.orderByAsc(Notification::getType);
                    } else {
                        wrapper.orderByDesc(Notification::getType);
                    }
                    // 同类型按时间排序
                    wrapper.orderByDesc(Notification::getCreatedAt);
                    break;
                default:
                    wrapper.orderByDesc(Notification::getCreatedAt);
            }
        } else {
            wrapper.orderByDesc(Notification::getCreatedAt);
        }
        
        return notificationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此通知");
        }
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false);
        
        List<Notification> notifications = notificationMapper.selectList(wrapper);
        LocalDateTime now = LocalDateTime.now();
        
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setReadAt(now);
            notificationMapper.updateById(notification);
        }
        
        log.info("用户 {} 标记了 {} 条通知为已读", userId, notifications.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此通知");
        }
        
        notificationMapper.deleteById(notificationId);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false);
        
        return notificationMapper.selectCount(wrapper);
    }
}
