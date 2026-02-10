package com.teamup.server.common.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询接口（管理用途）
 */
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping
    public Result<Page<AuditLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String sortBy
    ) {
        Page<AuditLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(AuditLog::getResourceType, resourceType);
        }
        if (resourceId != null) {
            wrapper.eq(AuditLog::getResourceId, resourceId);
        }
        
        // 处理排序
        if (sortBy != null && !sortBy.isEmpty()) {
            String[] parts = sortBy.split("_");
            if (parts.length == 2) {
                String field = parts[0];
                String direction = parts[1];
                
                switch (field) {
                    case "id":
                        if ("asc".equals(direction)) wrapper.orderByAsc(AuditLog::getId);
                        else wrapper.orderByDesc(AuditLog::getId);
                        break;
                    case "username":
                        if ("asc".equals(direction)) wrapper.orderByAsc(AuditLog::getUsername);
                        else wrapper.orderByDesc(AuditLog::getUsername);
                        break;
                    case "action":
                        if ("asc".equals(direction)) wrapper.orderByAsc(AuditLog::getAction);
                        else wrapper.orderByDesc(AuditLog::getAction);
                        break;
                    case "resourceType":
                        if ("asc".equals(direction)) wrapper.orderByAsc(AuditLog::getResourceType);
                        else wrapper.orderByDesc(AuditLog::getResourceType);
                        break;
                    case "result":
                        if ("asc".equals(direction)) wrapper.orderByAsc(AuditLog::getResult);
                        else wrapper.orderByDesc(AuditLog::getResult);
                        break;
                    case "ipAddress":
                        if ("asc".equals(direction)) wrapper.orderByAsc(AuditLog::getIpAddress);
                        else wrapper.orderByDesc(AuditLog::getIpAddress);
                        break;
                    case "createdAt":
                        if ("asc".equals(direction)) wrapper.orderByAsc(AuditLog::getCreatedAt);
                        else wrapper.orderByDesc(AuditLog::getCreatedAt);
                        break;
                    default:
                        wrapper.orderByDesc(AuditLog::getCreatedAt);
                }
            } else {
                wrapper.orderByDesc(AuditLog::getCreatedAt);
            }
        } else {
            wrapper.orderByDesc(AuditLog::getCreatedAt);
        }
        
        Page<AuditLog> result = auditLogMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }
}

