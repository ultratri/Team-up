package com.teamup.server.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计日志定期清理任务
 * 每月自动清理旧的审计日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogCleanupTask {

    private final AuditLogMapper auditLogMapper;

    /**
     * 每月1号凌晨2点执行清理任务
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void cleanupOldAuditLogs() {
        log.info("开始执行审计日志清理任务");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 1. 删除90天前的文件操作日志
            LocalDateTime fileLogThreshold = now.minusDays(90);
            int fileLogsDeleted = auditLogMapper.deleteOldFileLogs(fileLogThreshold);
            log.info("删除了 {} 条90天前的文件操作日志", fileLogsDeleted);
            
            // 2. 删除180天前的模板操作日志
            LocalDateTime templateLogThreshold = now.minusDays(180);
            int templateLogsDeleted = auditLogMapper.deleteOldTemplateLogs(templateLogThreshold);
            log.info("删除了 {} 条180天前的模板操作日志", templateLogsDeleted);
            
            // 3. 统计剩余日志
            long remainingLogs = auditLogMapper.selectCount(null);
            log.info("审计日志清理完成，剩余 {} 条日志", remainingLogs);
            
        } catch (Exception e) {
            log.error("审计日志清理任务执行失败", e);
        }
    }
    
    /**
     * 手动触发清理（用于测试）
     */
    public void manualCleanup() {
        log.info("手动触发审计日志清理");
        cleanupOldAuditLogs();
    }
}
