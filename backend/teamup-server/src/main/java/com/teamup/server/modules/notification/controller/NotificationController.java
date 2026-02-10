package com.teamup.server.modules.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.notification.entity.Notification;
import com.teamup.server.modules.notification.service.NotificationService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Notification>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Long userId = UserContext.getCurrentUserId();
        Page<Notification> notifications = notificationService.getUserNotifications(
            userId, page, size, isRead, type, sortBy, sortOrder
        );
        return Result.success(notifications);
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.success(0L);
            }
            Long count = notificationService.getUnreadCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            // 未登录用户返回0
            return Result.success(0L);
        }
    }

    /**
     * 标记为已读
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    /**
     * 全部标记为已读
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAllAsRead() {
        Long userId = UserContext.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return Result.success();
    }
}
