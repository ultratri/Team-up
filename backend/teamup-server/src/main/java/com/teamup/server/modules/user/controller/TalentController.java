package com.teamup.server.modules.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.user.service.TalentService;
import com.teamup.server.modules.user.vo.TalentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 人才墙控制器
 * 提供人才列表查询接口
 */
@RestController
@RequestMapping("/talents")
@RequiredArgsConstructor
public class TalentController {
    
    private final TalentService talentService;
    
    /**
     * 获取人才列表
     * @param page 页码（默认1）
     * @param size 每页大小（默认12）
     * @param department 院系筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @param intention 组队意向筛选（可选）
     * @return 分页的人才列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<TalentVO>> getTalentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String intention) {
        
        Page<TalentVO> result = talentService.getTalentList(
            page, size, department, keyword, intention);
        return Result.success(result);
    }
}
