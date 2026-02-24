package com.teamup.server.modules.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.user.service.TalentService;
import com.teamup.server.modules.user.vo.TalentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 人才墙控制器
 */
@RestController
@RequestMapping("/talents")
@RequiredArgsConstructor
public class TalentController {

    private final TalentService talentService;

    /**
     * 获取人才列表（人才墙）
     * @param page 页码
     * @param size 每页大小
     * @param department 院系筛选
     * @param keyword 关键词搜索
     * @param intention 组队意向筛选
     * @return 人才列表
     */
    @GetMapping
    public Result<Page<TalentVO>> getTalentList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String intention
    ) {
        Page<TalentVO> result = talentService.getTalentList(page, size, department, keyword, intention);
        return Result.success(result);
    }
}
