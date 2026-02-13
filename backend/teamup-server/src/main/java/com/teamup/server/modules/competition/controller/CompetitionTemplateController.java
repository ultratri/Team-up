package com.teamup.server.modules.competition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.audit.AuditLogService;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.entity.CompetitionTemplate;
import com.teamup.server.modules.competition.service.CompetitionService;
import com.teamup.server.modules.competition.service.CompetitionTemplateService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/competition-templates")
@RequiredArgsConstructor
public class CompetitionTemplateController {

    private final CompetitionTemplateService templateService;
    private final CompetitionService competitionService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Page<CompetitionTemplate>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        Page<CompetitionTemplate> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CompetitionTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CompetitionTemplate::getName, keyword);
        }
        wrapper.orderByDesc(CompetitionTemplate::getUpdatedAt);
        return Result.success(templateService.page(pageParam, wrapper));
    }

    /**
     * 保存比赛为模板
     * body: { name: string, payload: any }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<CompetitionTemplate> create(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getCurrentUserId();
        String name = body.get("name") != null ? String.valueOf(body.get("name")) : null;
        Object payload = body.get("payload");
        if (!StringUtils.hasText(name) || payload == null) {
            return Result.error(400, "模板名称与内容不能为空");
        }
        try {
            CompetitionTemplate t = new CompetitionTemplate();
            t.setName(name);
            t.setPayload(objectMapper.writeValueAsString(payload));
            t.setCreatedBy(userId);
            templateService.save(t);
            auditLogService.logSensitiveOperation("CREATE_COMPETITION_TEMPLATE", "COMPETITION_TEMPLATE", t.getId(),
                    "创建比赛模板：" + name, "SUCCESS", null);
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(500, "创建模板失败: " + e.getMessage());
        }
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<CompetitionTemplate> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        CompetitionTemplate t = templateService.getById(id);
        if (t == null) return Result.error(404, "模板不存在");
        
        String name = body.get("name") != null ? String.valueOf(body.get("name")) : null;
        Object payload = body.get("payload");
        if (!StringUtils.hasText(name) || payload == null) {
            return Result.error(400, "模板名称与内容不能为空");
        }
        try {
            t.setName(name);
            t.setPayload(objectMapper.writeValueAsString(payload));
            templateService.updateById(t);
            auditLogService.logSensitiveOperation("UPDATE_COMPETITION_TEMPLATE", "COMPETITION_TEMPLATE", t.getId(),
                    "更新比赛模板：" + name, "SUCCESS", null);
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(500, "更新模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        CompetitionTemplate t = templateService.getById(id);
        if (t == null) return Result.error(404, "模板不存在");
        
        templateService.removeById(id);
        auditLogService.logSensitiveOperation("DELETE_COMPETITION_TEMPLATE", "COMPETITION_TEMPLATE", id,
                "删除比赛模板：" + t.getName(), "SUCCESS", null);
        return Result.success();
    }

    /**
     * 从模板创建比赛（默认 DRAFT）
     * body: { templateId, name?, signupStartAt?, signupEndAt?, startAt?, endAt? }
     */
    @PostMapping("/{id}/create-competition")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Competition> createCompetitionFromTemplate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> overrides
    ) {
        CompetitionTemplate t = templateService.getById(id);
        if (t == null) return Result.error(404, "模板不存在");
        try {
            Competition c = objectMapper.readValue(t.getPayload(), Competition.class);
            c.setId(null);
            c.setStatus("DRAFT");
            c.setCreatedBy(UserContext.getCurrentUserId());

            if (overrides != null) {
                if (overrides.get("name") != null) c.setName(String.valueOf(overrides.get("name")));
                if (overrides.get("signupStartAt") != null) c.setSignupStartAt(java.time.LocalDateTime.parse(String.valueOf(overrides.get("signupStartAt"))));
                if (overrides.get("signupEndAt") != null) c.setSignupEndAt(java.time.LocalDateTime.parse(String.valueOf(overrides.get("signupEndAt"))));
                if (overrides.get("startAt") != null) c.setStartAt(java.time.LocalDateTime.parse(String.valueOf(overrides.get("startAt"))));
                if (overrides.get("endAt") != null) c.setEndAt(java.time.LocalDateTime.parse(String.valueOf(overrides.get("endAt"))));
            }

            competitionService.save(c);
            auditLogService.logSensitiveOperation("CREATE_COMPETITION_FROM_TEMPLATE", "COMPETITION", c.getId(),
                    "从模板创建比赛：" + t.getName(), "SUCCESS", null);
            return Result.success(c);
        } catch (Exception e) {
            return Result.error(500, "从模板创建比赛失败: " + e.getMessage());
        }
    }
}

