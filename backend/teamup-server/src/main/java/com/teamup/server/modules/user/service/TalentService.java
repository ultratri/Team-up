package com.teamup.server.modules.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.user.vo.TalentVO;

/**
 * 人才墙服务接口
 */
public interface TalentService {
    
    /**
     * 获取人才列表
     * @param page 页码
     * @param size 每页大小
     * @param department 院系筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @param intention 组队意向筛选（可选）
     * @return 分页的人才列表
     */
    Page<TalentVO> getTalentList(Integer page, Integer size, String department, String keyword, String intention);
}
