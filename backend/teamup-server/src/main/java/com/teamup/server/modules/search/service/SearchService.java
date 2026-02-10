package com.teamup.server.modules.search.service;

import com.teamup.server.modules.search.dto.SearchResultDTO;

/**
 * 搜索服务接口
 */
public interface SearchService {
    /**
     * 全局搜索
     */
    SearchResultDTO globalSearch(String keyword, Long userId);
}
