package com.teamup.server.modules.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.notification.dto.AnnouncementRequest;
import com.teamup.server.modules.notification.entity.Announcement;
import com.teamup.server.modules.notification.entity.Notification;
import com.teamup.server.modules.notification.mapper.AnnouncementMapper;
import com.teamup.server.modules.notification.mapper.NotificationMapper;
import com.teamup.server.modules.notification.service.AnnouncementService;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAnnouncement(AnnouncementRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 先保存到公告表
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        announcement.setIsActive(true);
        announcement.setPublisherId(currentUserId);
        announcement.setPublishedAt(now);
        announcementMapper.insert(announcement);
        
        // 2. 确定接收者列表
        List<Long> userIds;
        if ("ALL".equals(request.getReceiverType())) {
            // 发送给所有活跃用户
            userIds = userMapper.selectActiveUserIds(10000); // 最多10000个用户
        } else if ("SPECIFIC".equals(request.getReceiverType())) {
            // 发送给指定用户
            userIds = request.getUserIds();
            if (userIds == null || userIds.isEmpty()) {
                throw new RuntimeException("指定用户列表不能为空");
            }
        } else {
            throw new RuntimeException("无效的接收者类型");
        }

        // 3. 批量创建通知
        String type = request.getNotificationType() != null ? request.getNotificationType() : "SYSTEM_ANNOUNCEMENT";
        String priority = request.getPriority() != null ? request.getPriority() : "MEDIUM";
        
        // 在标题前添加优先级标识
        String title = request.getTitle();
        if ("HIGH".equals(priority)) {
            title = "【重要】" + title;
        }

        for (Long userId : userIds) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);  // 使用前端传来的通知类型
            notification.setTitle(title);
            notification.setContent(request.getContent());
            notification.setRelatedType("ANNOUNCEMENT");
            notification.setRelatedId(announcement.getId());
            notification.setIsRead(false);
            notification.setCreatedAt(now);
            notificationMapper.insert(notification);
        }

        log.info("管理员 {} 发布了公告 {}，类型 {}，发送给 {} 个用户", currentUserId, announcement.getId(), type, userIds.size());
    }

    @Override
    public Page<Announcement> getAnnouncements(int page, int size, String keyword, String sortBy, String sortOrder) {
        Page<Announcement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                .like(Announcement::getTitle, keyword)
                .or()
                .like(Announcement::getContent, keyword)
            );
        }
        
        // 排序
        if (sortBy != null && !sortBy.isEmpty()) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            switch (sortBy) {
                case "id":
                    if (isAsc) {
                        wrapper.orderByAsc(Announcement::getId);
                    } else {
                        wrapper.orderByDesc(Announcement::getId);
                    }
                    break;
                case "priority":
                    // 优先级排序：HIGH > MEDIUM > LOW
                    if (isAsc) {
                        wrapper.orderByAsc(Announcement::getPriority);
                    } else {
                        wrapper.orderByDesc(Announcement::getPriority);
                    }
                    // 同优先级按发布时间排序
                    wrapper.orderByDesc(Announcement::getPublishedAt);
                    break;
                case "publishedAt":
                default:
                    if (isAsc) {
                        wrapper.orderByAsc(Announcement::getPublishedAt);
                    } else {
                        wrapper.orderByDesc(Announcement::getPublishedAt);
                    }
                    break;
            }
        } else {
            wrapper.orderByDesc(Announcement::getPublishedAt);
        }
        
        return announcementMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        
        // 删除公告
        announcementMapper.deleteById(id);
        
        // 删除相关的通知
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getRelatedType, "ANNOUNCEMENT")
               .eq(Notification::getRelatedId, id);
        notificationMapper.delete(wrapper);
        
        log.info("管理员 {} 删除了公告 {}", UserContext.getCurrentUserId(), id);
    }
}
