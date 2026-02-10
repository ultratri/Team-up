package com.teamup.server.modules.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.notification.dto.AnnouncementRequest;
import com.teamup.server.modules.notification.entity.Announcement;

/**
 * 公告服务接口
 */
public interface AnnouncementService {
    /**
     * 创建公告
     */
    void createAnnouncement(AnnouncementRequest request);

    /**
     * 获取公告列表（管理员查看）
     */
    Page<Announcement> getAnnouncements(int page, int size, String keyword, String sortBy, String sortOrder);

    /**
     * 删除公告
     */
    void deleteAnnouncement(Long id);
}
