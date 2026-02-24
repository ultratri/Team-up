package com.teamup.server.modules.system.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.system.service.ExportService;
import com.teamup.server.modules.system.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class ExportController {

    private final ExportService exportService;
    private final ImportService importService;

    /**
     * 导出数据
     * @param type 数据类型: users, projects, teams, competitions, notifications, audit-logs
     * @param format 格式: excel, csv
     */
    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,
            @RequestParam(defaultValue = "excel") String format) {

        // 生成文件
        byte[] data = exportService.exportData(type, format);

        // 设置文件扩展名
        String extension = "excel".equalsIgnoreCase(format) ? "xlsx" : "csv";

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                type + "_" + LocalDate.now() + "." + extension);
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * 获取各类型数据的统计数量
     */
    @GetMapping("/export/counts")
    public Result<Map<String, Long>> getCounts() {
        Map<String, Long> counts = exportService.getDataCounts();
        return Result.success(counts);
    }

    /**
     * 导入数据
     * @param type 数据类型: users, projects, teams, competitions
     * @param file 上传的文件
     */
    @PostMapping("/import/{type}")
    public Result<Map<String, Integer>> importData(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        try {
            Map<String, Integer> result = importService.importData(type, file);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
