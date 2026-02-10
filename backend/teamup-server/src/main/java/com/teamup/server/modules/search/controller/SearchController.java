package com.teamup.server.modules.search.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.search.dto.SearchResultDTO;
import com.teamup.server.modules.search.service.SearchService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 全局搜索
     */
    @GetMapping("/global")
    @PreAuthorize("isAuthenticated()")
    public Result<SearchResultDTO> globalSearch(@RequestParam String keyword) {
        Long userId = UserContext.getCurrentUserId();
        SearchResultDTO result = searchService.globalSearch(keyword, userId);
        return Result.success(result);
    }
}
