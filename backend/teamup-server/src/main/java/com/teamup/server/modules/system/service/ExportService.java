package com.teamup.server.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opencsv.CSVWriter;
import com.teamup.server.common.audit.AuditLog;
import com.teamup.server.common.audit.AuditLogMapper;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.mapper.CompetitionMapper;
import com.teamup.server.modules.notification.entity.Announcement;
import com.teamup.server.modules.notification.mapper.AnnouncementMapper;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final TeamMapper teamMapper;
    private final CompetitionMapper competitionMapper;
    private final AnnouncementMapper announcementMapper;
    private final AuditLogMapper auditLogMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取各类型数据的统计数量（排除已删除/归档数据）
     */
    public Map<String, Long> getDataCounts() {
        Map<String, Long> counts = new HashMap<>();
        
        // 只统计活跃用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ne(User::getStatus, "BANNED");
        counts.put("users", userMapper.selectCount(userWrapper));
        
        // 只统计非归档项目
        LambdaQueryWrapper<Project> projectWrapper = new LambdaQueryWrapper<>();
        projectWrapper.ne(Project::getStatus, "ARCHIVED");
        counts.put("projects", projectMapper.selectCount(projectWrapper));
        
        // 团队无软删除，统计全部
        counts.put("teams", teamMapper.selectCount(null));
        
        // 只统计已发布的比赛
        LambdaQueryWrapper<Competition> competitionWrapper = new LambdaQueryWrapper<>();
        competitionWrapper.eq(Competition::getStatus, "PUBLISHED");
        counts.put("competitions", competitionMapper.selectCount(competitionWrapper));
        
        // 只统计活跃的公告
        LambdaQueryWrapper<Announcement> announcementWrapper = new LambdaQueryWrapper<>();
        announcementWrapper.eq(Announcement::getIsActive, true);
        counts.put("announcements", announcementMapper.selectCount(announcementWrapper));
        
        counts.put("audit-logs", auditLogMapper.selectCount(null));
        
        return counts;
    }

    /**
     * 导出数据
     */
    public byte[] exportData(String type, String format) {
        try {
            if ("excel".equalsIgnoreCase(format)) {
                return exportToExcel(type);
            } else if ("csv".equalsIgnoreCase(format)) {
                return exportToCsv(type);
            } else {
                throw new IllegalArgumentException("不支持的格式: " + format);
            }
        } catch (Exception e) {
            log.error("导出数据失败: type={}, format={}", type, format, e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 导出为 Excel
     */
    private byte[] exportToExcel(String type) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(type);

            // 创建标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            switch (type) {
                case "users":
                    exportUsersToExcel(sheet, headerStyle);
                    break;
                case "projects":
                    exportProjectsToExcel(sheet, headerStyle);
                    break;
                case "teams":
                    exportTeamsToExcel(sheet, headerStyle);
                    break;
                case "competitions":
                    exportCompetitionsToExcel(sheet, headerStyle);
                    break;
                case "announcements":
                    exportAnnouncementsToExcel(sheet, headerStyle);
                    break;
                case "audit-logs":
                    exportAuditLogsToExcel(sheet, headerStyle);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导出类型: " + type);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 导出为 CSV
     */
    private byte[] exportToCsv(String type) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            switch (type) {
                case "users":
                    exportUsersToCsv(csvWriter);
                    break;
                case "projects":
                    exportProjectsToCsv(csvWriter);
                    break;
                case "teams":
                    exportTeamsToCsv(csvWriter);
                    break;
                case "competitions":
                    exportCompetitionsToCsv(csvWriter);
                    break;
                case "announcements":
                    exportAnnouncementsToCsv(csvWriter);
                    break;
                case "audit-logs":
                    exportAuditLogsToCsv(csvWriter);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导出类型: " + type);
            }

            return stringWriter.toString().getBytes("UTF-8");
        }
    }

    // ==================== Excel 导出方法 ====================

    private void exportUsersToExcel(Sheet sheet, CellStyle headerStyle) {
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "学号/工号", "用户名", "邮箱", "手机号", "角色", "状态", "创建时间", "最后登录"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 分页查询并写入数据（排除已封禁用户）
        int rowNum = 1;
        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<User> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ne(User::getStatus, "BANNED");
            Page<User> result = userMapper.selectPage(page, wrapper);

            for (User user : result.getRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUserCode());
                row.createCell(2).setCellValue(user.getUsername());
                row.createCell(3).setCellValue(user.getEmail());
                row.createCell(4).setCellValue(user.getPhone() != null ? user.getPhone() : "");
                row.createCell(5).setCellValue(user.getRoles() != null ? String.join(",", user.getRoles()) : "");
                row.createCell(6).setCellValue(user.getStatus());
                row.createCell(7).setCellValue(formatDateTime(user.getCreatedAt()));
                row.createCell(8).setCellValue(formatDateTime(user.getLastLoginAt()));
            }

            if (!result.hasNext()) break;
            currentPage++;
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void exportProjectsToExcel(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "项目标题", "项目类型", "描述", "状态", "创建者ID", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Project> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.ne(Project::getStatus, "ARCHIVED");
            Page<Project> result = projectMapper.selectPage(page, wrapper);

            for (Project project : result.getRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(project.getId());
                row.createCell(1).setCellValue(project.getTitle());
                row.createCell(2).setCellValue(project.getProjectType() != null ? project.getProjectType() : "");
                row.createCell(3).setCellValue(project.getDescription() != null ? project.getDescription() : "");
                row.createCell(4).setCellValue(project.getStatus());
                row.createCell(5).setCellValue(project.getCreatorId());
                row.createCell(6).setCellValue(formatDateTime(project.getCreatedAt()));
            }

            if (!result.hasNext()) break;
            currentPage++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void exportTeamsToExcel(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "团队名称", "类型", "描述", "队长ID", "项目ID", "比赛ID", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Team> page = new Page<>(currentPage, pageSize);
            Page<Team> result = teamMapper.selectPage(page, new LambdaQueryWrapper<>());

            for (Team team : result.getRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(team.getId());
                row.createCell(1).setCellValue(team.getTeamName());
                // 将team_nature转换回type：TEMPORARY->PROJECT, LONG_TERM->COMPETITION
                String type = "LONG_TERM".equals(team.getTeamNature()) ? "COMPETITION" : "PROJECT";
                row.createCell(2).setCellValue(type);
                row.createCell(3).setCellValue(team.getDescription() != null ? team.getDescription() : "");
                row.createCell(4).setCellValue(team.getLeaderId());
                row.createCell(5).setCellValue(team.getProjectId() != null ? team.getProjectId() : 0);
                row.createCell(6).setCellValue(team.getCompetitionId() != null ? team.getCompetitionId() : 0);
                row.createCell(7).setCellValue(formatDateTime(team.getCreatedAt()));
            }

            if (!result.hasNext()) break;
            currentPage++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void exportCompetitionsToExcel(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "比赛名称", "主办方", "级别", "状态", "报名开始", "报名结束", "比赛开始", "比赛结束"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Competition> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
            // 只导出已发布的比赛
            wrapper.eq(Competition::getStatus, "PUBLISHED");
            Page<Competition> result = competitionMapper.selectPage(page, wrapper);

            for (Competition comp : result.getRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(comp.getId());
                row.createCell(1).setCellValue(comp.getName());
                row.createCell(2).setCellValue(comp.getOrganizer());
                row.createCell(3).setCellValue(comp.getLevel());
                row.createCell(4).setCellValue(comp.getStatus());
                row.createCell(5).setCellValue(formatDateTime(comp.getSignupStartAt()));
                row.createCell(6).setCellValue(formatDateTime(comp.getSignupEndAt()));
                row.createCell(7).setCellValue(formatDateTime(comp.getStartAt()));
                row.createCell(8).setCellValue(formatDateTime(comp.getEndAt()));
            }

            if (!result.hasNext()) break;
            currentPage++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void exportAnnouncementsToExcel(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "标题", "内容", "优先级", "是否活跃", "发布者ID", "发布时间", "过期时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Announcement> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
            // 只导出活跃的公告
            wrapper.eq(Announcement::getIsActive, true);
            wrapper.orderByDesc(Announcement::getPublishedAt);
            Page<Announcement> result = announcementMapper.selectPage(page, wrapper);

            for (Announcement announcement : result.getRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(announcement.getId());
                row.createCell(1).setCellValue(announcement.getTitle());
                row.createCell(2).setCellValue(announcement.getContent() != null ? announcement.getContent() : "");
                row.createCell(3).setCellValue(announcement.getPriority());
                row.createCell(4).setCellValue(announcement.getIsActive() ? "是" : "否");
                row.createCell(5).setCellValue(announcement.getPublisherId());
                row.createCell(6).setCellValue(formatDateTime(announcement.getPublishedAt()));
                row.createCell(7).setCellValue(formatDateTime(announcement.getExpiresAt()));
            }

            if (!result.hasNext()) break;
            currentPage++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void exportAuditLogsToExcel(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "操作者", "操作类型", "资源类型", "资源ID", "详情", "结果", "IP地址", "操作时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<AuditLog> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(AuditLog::getCreatedAt);
            Page<AuditLog> result = auditLogMapper.selectPage(page, wrapper);

            for (AuditLog log : result.getRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(2).setCellValue(log.getAction());
                row.createCell(3).setCellValue(log.getResourceType());
                row.createCell(4).setCellValue(log.getResourceId() != null ? log.getResourceId() : 0);
                row.createCell(5).setCellValue(log.getDetails() != null ? log.getDetails() : "");
                row.createCell(6).setCellValue(log.getResult());
                row.createCell(7).setCellValue(log.getIpAddress() != null ? log.getIpAddress() : "");
                row.createCell(8).setCellValue(formatDateTime(log.getCreatedAt()));
            }

            if (!result.hasNext()) break;
            currentPage++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ==================== CSV 导出方法 ====================

    private void exportUsersToCsv(CSVWriter writer) {
        writer.writeNext(new String[]{"ID", "学号/工号", "用户名", "邮箱", "手机号", "角色", "状态", "创建时间", "最后登录"});

        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<User> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ne(User::getStatus, "BANNED");
            Page<User> result = userMapper.selectPage(page, wrapper);

            for (User user : result.getRecords()) {
                writer.writeNext(new String[]{
                        String.valueOf(user.getId()),
                        user.getUserCode(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhone() != null ? user.getPhone() : "",
                        user.getRoles() != null ? String.join(",", user.getRoles()) : "",
                        user.getStatus(),
                        formatDateTime(user.getCreatedAt()),
                        formatDateTime(user.getLastLoginAt())
                });
            }

            if (!result.hasNext()) break;
            currentPage++;
        }
    }

    private void exportProjectsToCsv(CSVWriter writer) {
        writer.writeNext(new String[]{"ID", "项目标题", "项目类型", "描述", "状态", "创建者ID", "创建时间"});

        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Project> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.ne(Project::getStatus, "ARCHIVED");
            Page<Project> result = projectMapper.selectPage(page, wrapper);

            for (Project project : result.getRecords()) {
                writer.writeNext(new String[]{
                        String.valueOf(project.getId()),
                        project.getTitle(),
                        project.getProjectType() != null ? project.getProjectType() : "",
                        project.getDescription() != null ? project.getDescription() : "",
                        project.getStatus(),
                        String.valueOf(project.getCreatorId()),
                        formatDateTime(project.getCreatedAt())
                });
            }

            if (!result.hasNext()) break;
            currentPage++;
        }
    }

    private void exportTeamsToCsv(CSVWriter writer) {
        writer.writeNext(new String[]{"ID", "团队名称", "类型", "描述", "队长ID", "项目ID", "比赛ID", "创建时间"});

        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Team> page = new Page<>(currentPage, pageSize);
            Page<Team> result = teamMapper.selectPage(page, new LambdaQueryWrapper<>());

            for (Team team : result.getRecords()) {
                writer.writeNext(new String[]{
                        String.valueOf(team.getId()),
                        team.getTeamName(),
                        // 将team_nature转换回type：TEMPORARY->PROJECT, LONG_TERM->COMPETITION
                        "LONG_TERM".equals(team.getTeamNature()) ? "COMPETITION" : "PROJECT",
                        team.getDescription() != null ? team.getDescription() : "",
                        String.valueOf(team.getLeaderId()),
                        team.getProjectId() != null ? String.valueOf(team.getProjectId()) : "",
                        team.getCompetitionId() != null ? String.valueOf(team.getCompetitionId()) : "",
                        formatDateTime(team.getCreatedAt())
                });
            }

            if (!result.hasNext()) break;
            currentPage++;
        }
    }

    private void exportCompetitionsToCsv(CSVWriter writer) {
        writer.writeNext(new String[]{"ID", "比赛名称", "主办方", "级别", "状态", "报名开始", "报名结束", "比赛开始", "比赛结束"});

        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Competition> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
            // 只导出已发布的比赛
            wrapper.eq(Competition::getStatus, "PUBLISHED");
            Page<Competition> result = competitionMapper.selectPage(page, wrapper);

            for (Competition comp : result.getRecords()) {
                writer.writeNext(new String[]{
                        String.valueOf(comp.getId()),
                        comp.getName(),
                        comp.getOrganizer(),
                        comp.getLevel(),
                        comp.getStatus(),
                        formatDateTime(comp.getSignupStartAt()),
                        formatDateTime(comp.getSignupEndAt()),
                        formatDateTime(comp.getStartAt()),
                        formatDateTime(comp.getEndAt())
                });
            }

            if (!result.hasNext()) break;
            currentPage++;
        }
    }

    private void exportAnnouncementsToCsv(CSVWriter writer) {
        writer.writeNext(new String[]{"ID", "标题", "内容", "优先级", "是否活跃", "发布者ID", "发布时间", "过期时间"});

        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<Announcement> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
            // 只导出活跃的公告
            wrapper.eq(Announcement::getIsActive, true);
            wrapper.orderByDesc(Announcement::getPublishedAt);
            Page<Announcement> result = announcementMapper.selectPage(page, wrapper);

            for (Announcement announcement : result.getRecords()) {
                writer.writeNext(new String[]{
                        String.valueOf(announcement.getId()),
                        announcement.getTitle(),
                        announcement.getContent() != null ? announcement.getContent() : "",
                        announcement.getPriority(),
                        announcement.getIsActive() ? "是" : "否",
                        String.valueOf(announcement.getPublisherId()),
                        formatDateTime(announcement.getPublishedAt()),
                        formatDateTime(announcement.getExpiresAt())
                });
            }

            if (!result.hasNext()) break;
            currentPage++;
        }
    }

    private void exportAuditLogsToCsv(CSVWriter writer) {
        writer.writeNext(new String[]{"ID", "操作者", "操作类型", "资源类型", "资源ID", "详情", "结果", "IP地址", "操作时间"});

        int pageSize = 1000;
        int currentPage = 1;

        while (true) {
            Page<AuditLog> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(AuditLog::getCreatedAt);
            Page<AuditLog> result = auditLogMapper.selectPage(page, wrapper);

            for (AuditLog log : result.getRecords()) {
                writer.writeNext(new String[]{
                        String.valueOf(log.getId()),
                        log.getUsername() != null ? log.getUsername() : "",
                        log.getAction(),
                        log.getResourceType(),
                        log.getResourceId() != null ? String.valueOf(log.getResourceId()) : "",
                        log.getDetails() != null ? log.getDetails() : "",
                        log.getResult(),
                        log.getIpAddress() != null ? log.getIpAddress() : "",
                        formatDateTime(log.getCreatedAt())
                });
            }

            if (!result.hasNext()) break;
            currentPage++;
        }
    }

    // ==================== 工具方法 ====================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }
}
