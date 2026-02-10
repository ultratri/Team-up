package com.teamup.server.modules.system.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.system.entity.DepartmentMajor;
import com.teamup.server.modules.system.service.DepartmentMajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/department-major")
@RequiredArgsConstructor
public class DepartmentMajorController {

    private final DepartmentMajorService service;

    /**
     * 公共接口：获取院系->专业列表（仅 enabled=1）
     */
    @GetMapping("/tree")
    public Result<Map<String, List<String>>> tree() {
        return Result.success(service.getDepartmentMajorTree());
    }

    /**
     * 管理端：获取全部记录
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<List<DepartmentMajor>> adminList() {
        return Result.success(service.listAll());
    }

    /**
     * 管理端：新增
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<DepartmentMajor> create(@RequestBody DepartmentMajor item) {
        return Result.success(service.create(item));
    }

    /**
     * 管理端：更新
     */
    @PutMapping("/admin")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<DepartmentMajor> update(@RequestBody DepartmentMajor item) {
        return Result.success(service.update(item));
    }

    /**
     * 管理端：删除
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}
