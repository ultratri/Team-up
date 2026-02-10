package com.teamup.server.modules.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.notification.dto.AnnouncementRequest;
import com.teamup.server.modules.notification.entity.Announcement;
import com.teamup.server.modules.notification.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 公告管理控制器（管理员专用）
 */
@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 发布公告（发送给所有用户或指定用户）
     */
    @PostMapping
    public Result<Void> createAnnouncement(@RequestBody AnnouncementRequest request) {
        announcementService.createAnnouncement(request);
        return Result.success();
    }

    /**
     * 获取公告列表（管理员查看所有已发布的公告）
     */
    @GetMapping
    public Result<Page<Announcement>> getAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Page<Announcement> announcements = announcementService.getAnnouncements(
            page, size, keyword, sortBy, sortOrder
        );
        return Result.success(announcements);
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }
}
